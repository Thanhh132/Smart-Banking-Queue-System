import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../config/api.config';

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
  profileComplete: boolean;
  passwordChangeAvailable: boolean;
}

export interface AccountChangeConfirmation {
  status: 'PENDING_NEW_EMAIL' | 'APPLIED';
  message: string;
  emailChanged: boolean;
}

export interface CustomerProfileField {
  key: string;
  label: string;
  type: 'text' | 'date' | 'number' | 'textarea';
  placeholder: string;
  required: boolean;
}

export interface CustomerPaperlessProfile {
  values: Record<string, string>;
  requiredFields: CustomerProfileField[];
  missingFields: string[];
  complete: boolean;
}

@Injectable({ providedIn: 'root' })
export class AccountService {
  private http = inject(HttpClient);
  private apiUrl = `${inject(API_BASE_URL)}/account`;

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

  getPaperlessProfile(serviceId?: number) {
    if (!serviceId) {
      return this.http.get<CustomerPaperlessProfile>(`${this.apiUrl}/paperless-profile`);
    }

    return this.http.get<CustomerPaperlessProfile>(`${this.apiUrl}/paperless-profile`, {
      params: { serviceId },
    });
  }

  updatePaperlessProfile(payload: { serviceId?: number; values: Record<string, string> }) {
    return this.http.put<CustomerPaperlessProfile>(`${this.apiUrl}/paperless-profile`, payload);
  }

  completeSocialProfile(payload: {
    fullName: string;
    phone: string;
    permanentAddress: string;
    contactAddress: string;
  }) {
    return this.http.put<AccountProfile>(`${this.apiUrl}/social-profile`, payload);
  }
}
