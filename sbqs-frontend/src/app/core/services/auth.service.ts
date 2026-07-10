import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, tap } from 'rxjs';

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
  private apiUrl = 'http://localhost:8081/api/auth';

  login(payload: { email: string; password: string }) {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, payload).pipe(
      tap((response) => {
        this.saveSession(response, true);
      })
    );
  }

  refresh() {
    const refreshToken = localStorage.getItem('refreshToken');

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

  logout(): Observable<void> {
    const refreshToken = localStorage.getItem('refreshToken');
    this.clearSession();

    if (!refreshToken) {
      return of(void 0);
    }

    return this.http.post<void>(`${this.apiUrl}/logout`, { refreshToken }).pipe(
      catchError(() => of(void 0))
    );
  }

  getAccessToken(): string {
    return localStorage.getItem('accessToken') || '';
  }

  private saveSession(response: LoginResponse, clearFirst: boolean): void {
    const selectedBranchId = localStorage.getItem('selectedBranchId');

    if (clearFirst) {
      this.clearSession();
    }

    localStorage.setItem('accessToken', response.accessToken);
    if (response.refreshToken) {
      localStorage.setItem('refreshToken', response.refreshToken);
    } else {
      localStorage.removeItem('refreshToken');
    }
    localStorage.setItem('userRole', response.role);
    localStorage.setItem('fullName', response.fullName);
    localStorage.setItem('email', response.email);
    localStorage.setItem('authenticationSource', response.authenticationSource);

    if (response.branchId) {
      localStorage.setItem('selectedBranchId', String(response.branchId));
    } else if (!clearFirst && selectedBranchId) {
      localStorage.setItem('selectedBranchId', selectedBranchId);
    }
  }

  private clearSession(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userRole');
    localStorage.removeItem('fullName');
    localStorage.removeItem('email');
    localStorage.removeItem('authenticationSource');
    localStorage.removeItem('selectedBranchId');
    localStorage.removeItem('currentTicket');
    localStorage.removeItem('customerAddress');
    localStorage.removeItem('customerLatitude');
    localStorage.removeItem('customerLongitude');
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('accessToken');
  }

  getRole(): string {
    return localStorage.getItem('userRole') || '';
  }

  updateDisplayName(fullName: string): void {
    localStorage.setItem('fullName', fullName);
  }

  clearLocalSession(): void {
    this.clearSession();
  }

  isFallbackSession(): boolean {
    return localStorage.getItem('authenticationSource') === 'FALLBACK';
  }

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
