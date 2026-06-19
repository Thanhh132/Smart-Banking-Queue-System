import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ApiErrorService {
  getMessage(error: any, fallback: string): string {
    if (error?.name === 'TimeoutError') {
      return 'May chu phan hoi qua lau. Vui long kiem tra backend va Keycloak.';
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

    if (lower.includes('email da ton tai') || lower.includes('email')) {
      return 'Email da ton tai. Vui long dung email khac.';
    }

    if (lower.includes('so dien thoai da ton tai') || lower.includes('phone')) {
      return 'So dien thoai da ton tai. Vui long dung so khac.';
    }

    if (lower.includes('invalid_grant')) {
      return 'Email hoac mat khau khong dung, hoac tai khoan Keycloak chua san sang.';
    }

    if (lower.includes('constraint') || lower.includes('foreign key')) {
      return 'Du lieu nay dang duoc su dung nen chua the xoa han.';
    }

    return message;
  }
}
