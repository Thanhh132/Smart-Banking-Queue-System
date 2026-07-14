import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';

export interface LoginResponse {
  accessToken: string;
  refreshToken: string | null;
  tokenType: string;
  expiresIn: number;
  role: string;
  fullName: string;
  email: string;
  branchId: number | null;
  authenticationSource: 'KEYCLOAK' | 'FALLBACK';
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl = `${inject(API_BASE_URL)}/auth`;

  login(payload: { email: string; password: string }) {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, payload).pipe(
      tap((response) => {
        this.saveSession(response, true);
      })
    );
  }

  /** Đổi refresh token lấy phiên mới và giữ nguyên ngữ cảnh chi nhánh đang chọn. */
  refresh() {
    const refreshToken = sessionStorage.getItem('refreshToken');

    return this.http.post<LoginResponse>(`${this.apiUrl}/refresh`, {
      refreshToken,
    }).pipe(
      tap((response) => {
        this.saveSession(response, false);
      })
    );
  }

  register(payload: {
    fullName: string;
    email: string;
    password: string;
    confirmPassword: string;
    phone: string;
  }) {
    return this.http.post(`${this.apiUrl}/register`, payload);
  }

  forgotPassword(email: string) {
    return this.http.post<void>(`${this.apiUrl}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string, confirmPassword: string) {
    return this.http.post<void>(`${this.apiUrl}/reset-password`, {
      token,
      newPassword,
      confirmPassword,
    });
  }

  verifyEmail(token: string) {
    return this.http.post<void>(`${this.apiUrl}/verify-email`, null, {
      params: { token },
    });
  }

  resendVerification(email: string) {
    return this.http.post<void>(`${this.apiUrl}/resend-verification`, { email });
  }

  /**
   * Xóa dữ liệu nhạy cảm ở trình duyệt ngay lập tức; lỗi thu hồi token phía server
   * không được giữ người dùng lại trong một phiên local đã yêu cầu đăng xuất.
   */
  logout(): Observable<void> {
    const refreshToken = sessionStorage.getItem('refreshToken');
    this.clearSession();

    if (!refreshToken) {
      return of(void 0);
    }

    return this.http.post<void>(`${this.apiUrl}/logout`, { refreshToken }).pipe(
      catchError(() => of(void 0))
    );
  }

  getAccessToken(): string {
    return sessionStorage.getItem('accessToken') || '';
  }

  /**
   * Chuẩn hóa response của Keycloak và fallback vào cùng một sessionStorage.
   * Khi refresh, selectedBranchId của CUSTOMER được giữ nếu token không mang branchId.
   */
  private saveSession(response: LoginResponse, clearFirst: boolean): void {
    const selectedBranchId = sessionStorage.getItem('selectedBranchId');

    if (clearFirst) {
      this.clearSession();
    }

    sessionStorage.setItem('accessToken', response.accessToken);
    if (response.refreshToken) {
      sessionStorage.setItem('refreshToken', response.refreshToken);
    } else {
      sessionStorage.removeItem('refreshToken');
    }
    sessionStorage.setItem('userRole', response.role);
    sessionStorage.setItem('fullName', response.fullName);
    sessionStorage.setItem('email', response.email);
    sessionStorage.setItem('authenticationSource', response.authenticationSource);

    if (response.branchId) {
      sessionStorage.setItem('selectedBranchId', String(response.branchId));
    } else if (!clearFirst && selectedBranchId) {
      sessionStorage.setItem('selectedBranchId', selectedBranchId);
    }
  }

  /** Xóa cả token lẫn dữ liệu hành trình khách hàng để tài khoản kế tiếp không dùng nhầm. */
  private clearSession(): void {
    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('refreshToken');
    sessionStorage.removeItem('userRole');
    sessionStorage.removeItem('fullName');
    sessionStorage.removeItem('email');
    sessionStorage.removeItem('authenticationSource');
    sessionStorage.removeItem('selectedBranchId');
    sessionStorage.removeItem('currentTicket');
    sessionStorage.removeItem('customerAddress');
    sessionStorage.removeItem('customerLatitude');
    sessionStorage.removeItem('customerLongitude');
  }

  isLoggedIn(): boolean {
    return !!sessionStorage.getItem('accessToken');
  }

  getRole(): string {
    return sessionStorage.getItem('userRole') || '';
  }

  updateDisplayName(fullName: string): void {
    sessionStorage.setItem('fullName', fullName);
  }

  clearLocalSession(): void {
    this.clearSession();
  }

  isFallbackSession(): boolean {
    return sessionStorage.getItem('authenticationSource') === 'FALLBACK';
  }

  /** Ánh xạ role thành landing page dùng chung cho login, guard và redirect. */
  getHomeRoute(role: string): string {
    switch (role) {
      case 'SUPER_ADMIN':
        return '/super-admin';
      case 'BRANCH_ADMIN':
        return '/admin';
      case 'STAFF':
        return '/staff';
      case 'CUSTOMER':
        return '/customer';
      default:
        return '/login';
    }
  }
}
