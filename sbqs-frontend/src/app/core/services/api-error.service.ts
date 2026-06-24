import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ApiErrorService {
  getMessage(error: any, fallback: string): string {
    if (error?.name === 'TimeoutError') {
      return 'Máy chủ phản hồi quá lâu. Vui lòng kiểm tra backend và Keycloak.';
    }

    const raw = error?.error?.message || error?.error || error?.message;

    if (!raw) {
      return fallback;
    }

    if (typeof raw === 'object') {
      return raw.message || fallback;
    }

    if (typeof raw !== 'string') {
      return fallback;
    }

    try {
      const parsed = JSON.parse(raw);
      return parsed.message || parsed.error_description || parsed.error || fallback;
    } catch {
      return this.humanize(raw);
    }
  }

  private humanize(message: string): string {
    const lower = message.toLowerCase();

    if (lower.includes('email da ton tai') || lower.includes('email đã tồn tại')) {
      return 'Email đã tồn tại. Vui lòng dùng email khác.';
    }

    if (lower.includes('so dien thoai da ton tai') || lower.includes('số điện thoại đã tồn tại') || lower.includes('phone')) {
      return 'Số điện thoại đã tồn tại. Vui lòng dùng số khác.';
    }

    if (lower.includes('invalid_grant')) {
      return 'Email hoặc mật khẩu không chính xác.';
    }

    return message;
  }
}
