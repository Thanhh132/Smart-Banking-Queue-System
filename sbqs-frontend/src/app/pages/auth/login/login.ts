import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { ApiErrorService } from '../../../core/services/api-error.service';

import { AppIcon } from '../../../shared/components/app-icon/app-icon';
import { PreventAutofillDirective } from '../../../shared/directives/prevent-autofill.directive';

interface DevLoginAccount {
  label: string;
  role: string;
  email: string;
  password: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, AppIcon, PreventAutofillDirective],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private authService = inject(AuthService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);
  private http = inject(HttpClient);

  isSubmitting = false;
  isGoogleSubmitting = false;
  errorMessage = '';
  showPassword = false;
  devAccounts: DevLoginAccount[] = [];

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  ngOnInit(): void {
    if (!['localhost', '127.0.0.1'].includes(window.location.hostname)) return;
    this.http.get<DevLoginAccount[]>('/dev-login-accounts.local.json').subscribe({
      next: (accounts) => {
        this.devAccounts = Array.isArray(accounts) ? accounts : [];
        this.cdr.detectChanges();
      },
      error: () => {
        this.devAccounts = [];
      },
    });
  }

  fillDevAccount(account: DevLoginAccount): void {
    this.loginForm.setValue({ email: account.email, password: account.password });
    this.loginForm.markAsPristine();
    this.loginForm.markAsUntouched();
    this.errorMessage = '';
    this.showPassword = false;
  }

  submit(): void {
    this.errorMessage = '';

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.cdr.detectChanges();
      return;
    }

    const payload = {
      email: this.loginForm.value.email || '',
      password: this.loginForm.value.password || '',
    };

    this.isSubmitting = true;

    this.authService
      .login(payload)
      .pipe(
        timeout(15000),
        finalize(() => {
          this.isSubmitting = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (response) => {
          this.router.navigateByUrl(this.authService.getPostLoginRoute(response));
        },
        error: (err) => {
          this.errorMessage = this.apiError.getMessage(
            err,
            'Đăng nhập thất bại. Vui lòng kiểm tra email và mật khẩu.',
          );
          this.cdr.detectChanges();
        },
      });
  }

  async loginWithGoogle(): Promise<void> {
    this.errorMessage = '';
    this.isGoogleSubmitting = true;
    try {
      const response = await this.authService.startGoogleLogin();
      if (response) {
        await this.router.navigateByUrl(this.authService.getPostLoginRoute(response));
      }
    } catch (error) {
      this.authService.clearLocalSession();
      this.errorMessage = error instanceof Error
        ? error.message
        : 'Không thể hoàn tất đăng nhập Google. Vui lòng thử lại.';
    } finally {
      this.isGoogleSubmitting = false;
      this.cdr.detectChanges();
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }
}
