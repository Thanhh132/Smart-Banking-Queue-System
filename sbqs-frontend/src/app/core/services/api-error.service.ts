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

    if (lower.includes('so dien thoai da ton tai') || lower.includes('số điện thoại đã tồn tại')) {
      return 'Số điện thoại đã tồn tại. Vui lòng dùng số khác.';
    }

    if (lower.includes('identity_provider') || lower.includes('sqlgrammar')) {
      return 'Cấu trúc dữ liệu hệ thống chưa được cập nhật. Vui lòng khởi động lại backend và thử lại.';
    }

    if (lower.includes('invalid_grant') || lower.includes('email hoac mat khau khong dung')) {
      return 'Email hoặc mật khẩu không chính xác.';
    }

    if (lower.includes('mat khau xac nhan khong khop')) {
      return 'Mật khẩu xác nhận không khớp.';
    }

    if (lower.includes('vui long xac minh email')) {
      return 'Tài khoản chưa xác minh email. Vui lòng kiểm tra hộp thư và kích hoạt tài khoản trước khi đăng nhập.';
    }

    if (lower.includes('tai khoan da bi khoa')) {
      return 'Tài khoản đang bị khóa. Vui lòng liên hệ quản trị viên.';
    }

    if (lower.includes('chua duoc gan role') || lower.includes('chưa được gán role')) {
      return 'Tài khoản chưa được gán quyền trong Keycloak. Vui lòng kiểm tra role SBQS của tài khoản.';
    }

    if (lower.includes('qua nhieu lan dang nhap')) {
      return message
        .replace('Qua nhieu lan dang nhap that bai', 'Đăng nhập sai quá nhiều lần')
        .replace('Vui long thu lai sau', 'Vui lòng thử lại sau')
        .replace('phut', 'phút')
        .replace('giay', 'giây');
    }

    return message;
  }
}
