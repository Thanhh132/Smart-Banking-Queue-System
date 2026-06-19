import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  role: string;
  fullName: string;
  email: string;
  branchId: number | null;
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
    phone: string;
  }) {
    return this.http.post(`${this.apiUrl}/register`, payload);
  }

  logout(): void {
    this.clearSession();
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
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('userRole', response.role);
    localStorage.setItem('fullName', response.fullName);
    localStorage.setItem('email', response.email);

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
    localStorage.removeItem('selectedBranchId');
    localStorage.removeItem('currentTicket');
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('accessToken');
  }

  getRole(): string {
    return localStorage.getItem('userRole') || '';
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
