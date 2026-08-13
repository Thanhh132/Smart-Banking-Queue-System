import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs';

import { AuthService, DevLoginAccount } from '../../../core/services/auth.service';
import { ApiErrorService } from '../../../core/services/api-error.service';

import { AppIcon } from '../../../shared/components/app-icon/app-icon';
import { PreventAutofillDirective } from '../../../shared/directives/prevent-autofill.directive';

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

  isSubmitting = false;
  isGoogleSubmitting = false;
  errorMessage = '';
  showPassword = false;
  devAccounts: DevLoginAccount[] = [];
  devLoginAvailable = false;
  quickLoginUserId: number | null = null;

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.loadDevLoginAccounts();
  }

  loadDevLoginAccounts(): void {
    this.authService.getDevLoginAccounts().subscribe({
      next: (accounts) => {
        this.devLoginAvailable = true;
        this.devAccounts = accounts;
        this.cdr.detectChanges();
      },
      error: () => {
        this.devLoginAvailable = false;
        this.devAccounts = [];
        this.cdr.detectChanges();
      },
    });
  }

  loginDevAccount(account: DevLoginAccount): void {
    if (!this.devLoginAvailable || this.quickLoginUserId !== null) return;
    this.errorMessage = '';
    this.quickLoginUserId = account.userId;
    this.authService.devLogin(account.userId).pipe(
      finalize(() => {
        this.quickLoginUserId = null;
        this.cdr.detectChanges();
      }),
    ).subscribe({
      next: (response) => this.router.navigateByUrl(this.authService.getPostLoginRoute(response)),
      error: (error) => {
        this.errorMessage = this.apiError.getMessage(error, 'Không thể đăng nhập nhanh tài khoản này.');
        this.cdr.detectChanges();
      },
    });
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
