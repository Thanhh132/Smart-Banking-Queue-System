import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, firstValueFrom, of, tap, timeout } from 'rxjs';
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
  profileComplete: boolean;
}

interface GoogleLoginConfig {
  enabled: boolean;
  authorizationEndpoint: string;
  clientId: string;
  redirectUri: string;
}

interface GoogleCallbackMessage {
  type: 'SBQS_GOOGLE_OAUTH_CALLBACK';
  code: string | null;
  state: string | null;
  error: string | null;
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

  async startGoogleLogin(): Promise<LoginResponse | null> {
    this.clearSession();
    this.clearGoogleOAuthSession();
    const state = this.randomBase64Url(24);
    const verifier = this.randomBase64Url(64);
    sessionStorage.setItem('googleOAuthState', state);
    sessionStorage.setItem('googlePkceVerifier', verifier);

    // Mở ngay trong thao tác click để trình duyệt không chặn popup.
    const popup = window.open(
      'about:blank',
      `sbqs-google-${state}`,
      this.googlePopupFeatures(),
    );

    try {
      const config = await firstValueFrom(
        this.http.get<GoogleLoginConfig>(`${this.apiUrl}/google/config`),
      );
      if (!config.enabled) {
        throw new Error('Đăng nhập Google chưa được cấu hình.');
      }

      const challengeBytes = await crypto.subtle.digest(
        'SHA-256',
        new TextEncoder().encode(verifier),
      );
      const challenge = this.bytesToBase64Url(new Uint8Array(challengeBytes));
      const params = new URLSearchParams({
        client_id: config.clientId,
        redirect_uri: config.redirectUri,
        response_type: 'code',
        scope: 'openid profile email',
        state,
        code_challenge: challenge,
        code_challenge_method: 'S256',
        kc_idp_hint: 'google',
        prompt: 'select_account',
      });
      const authorizationUrl = `${config.authorizationEndpoint}?${params.toString()}`;

      // Nếu popup bị chặn thì vẫn hỗ trợ luồng chuyển toàn bộ trang.
      if (!popup) {
        window.location.assign(authorizationUrl);
        return null;
      }

      return await this.waitForGooglePopup(popup, authorizationUrl, state, verifier);
    } catch (error) {
      popup?.close();
      this.clearGoogleOAuthSession();
      this.clearSession();
      throw error;
    }
  }

  exchangeGoogleCode(code: string, codeVerifier: string) {
    return this.http.post<LoginResponse>(`${this.apiUrl}/google/exchange`, {
      code,
      codeVerifier,
    }).pipe(tap((response) => this.saveSession(response, true)));
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
    sessionStorage.setItem('profileComplete', String(response.profileComplete));

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
    sessionStorage.removeItem('profileComplete');
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

  markProfileComplete(fullName: string): void {
    this.updateDisplayName(fullName);
    sessionStorage.setItem('profileComplete', 'true');
  }

  isProfileComplete(): boolean {
    return sessionStorage.getItem('profileComplete') !== 'false';
  }

  getPostLoginRoute(response: LoginResponse): string {
    if (response.role === 'CUSTOMER' && !response.profileComplete) {
      return '/complete-profile';
    }
    return this.getHomeRoute(response.role);
  }

  private randomBase64Url(byteLength: number): string {
    const bytes = new Uint8Array(byteLength);
    crypto.getRandomValues(bytes);
    return this.bytesToBase64Url(bytes);
  }

  private bytesToBase64Url(bytes: Uint8Array): string {
    let binary = '';
    bytes.forEach((byte) => (binary += String.fromCharCode(byte)));
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
  }

  clearLocalSession(): void {
    this.clearSession();
  }

  private waitForGooglePopup(
    popup: Window,
    authorizationUrl: string,
    expectedState: string,
    verifier: string,
  ): Promise<LoginResponse> {
    return new Promise<LoginResponse>((resolve, reject) => {
      let settled = false;

      const finish = (): void => {
        window.removeEventListener('message', handleMessage);
        window.clearInterval(closedWatcher);
        window.clearTimeout(expiryTimer);
        this.clearGoogleOAuthSession();
        popup.close();
      };

      const fail = (message: string): void => {
        if (settled) return;
        settled = true;
        finish();
        this.clearSession();
        reject(new Error(message));
      };

      const handleMessage = (event: MessageEvent<GoogleCallbackMessage>): void => {
        if (
          event.origin !== window.location.origin ||
          event.source !== popup ||
          event.data?.type !== 'SBQS_GOOGLE_OAUTH_CALLBACK'
        ) {
          return;
        }

        const { code, state, error } = event.data;
        if (error) {
          fail('Bạn đã hủy hoặc Google từ chối yêu cầu đăng nhập.');
          return;
        }
        if (!code || !state || state !== expectedState) {
          fail('Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn.');
          return;
        }

        settled = true;
        finish();
        firstValueFrom(this.exchangeGoogleCode(code, verifier).pipe(timeout(20000)))
          .then(resolve)
          .catch((exchangeError) => {
            this.clearSession();
            reject(exchangeError);
          });
      };

      const closedWatcher = window.setInterval(() => {
        if (popup.closed) {
          fail('Cửa sổ đăng nhập Google đã được đóng trước khi hoàn tất.');
        }
      }, 500);
      const expiryTimer = window.setTimeout(() => {
        fail('Phiên đăng nhập Google đã hết hạn. Vui lòng thử lại.');
      }, 120000);

      window.addEventListener('message', handleMessage);
      popup.location.assign(authorizationUrl);
      popup.focus();
    });
  }

  private googlePopupFeatures(): string {
    const width = 520;
    const height = 680;
    const left = Math.max(0, window.screenX + (window.outerWidth - width) / 2);
    const top = Math.max(0, window.screenY + (window.outerHeight - height) / 2);
    return [
      'popup=yes',
      `width=${width}`,
      `height=${height}`,
      `left=${Math.round(left)}`,
      `top=${Math.round(top)}`,
      'resizable=yes',
      'scrollbars=yes',
    ].join(',');
  }

  private clearGoogleOAuthSession(): void {
    sessionStorage.removeItem('googleOAuthState');
    sessionStorage.removeItem('googlePkceVerifier');
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
