import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export interface AccountProfile {
  userId: number;
  fullName: string;
  email: string;
  phone: string;
  role: string;
  status: string;
  branchId: number | null;
  branchName: string | null;
  createdAt: string;
}

export interface AccountChangeConfirmation {
  status: 'PENDING_NEW_EMAIL' | 'APPLIED';
  message: string;
  emailChanged: boolean;
}

@Injectable({ providedIn: 'root' })
export class AccountService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8081/api/account';

  getProfile() {
    return this.http.get<AccountProfile>(this.apiUrl);
  }

  /** Gửi yêu cầu thay đổi; backend chỉ lưu tạm và gửi link xác nhận về email hiện tại. */
  requestProfileChange(payload: { fullName: string; email: string; phone: string }) {
    return this.http.post<void>(`${this.apiUrl}/change-request`, payload);
  }

  /** Xác nhận token lấy từ link email; có thể dẫn tới bước xác minh email mới. */
  confirmProfileChange(token: string) {
    return this.http.post<AccountChangeConfirmation>(`${this.apiUrl}/confirm-change`, null, {
      params: { token },
    });
  }

  /** Đổi mật khẩu sau khi backend kiểm tra mật khẩu hiện tại với Keycloak. */
  changePassword(payload: { currentPassword: string; newPassword: string }) {
    return this.http.put<void>(`${this.apiUrl}/password`, payload);
  }
}
