import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnDestroy, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { ApiErrorService } from '../../../core/services/api-error.service';

import { AppIcon } from '../../../shared/components/app-icon/app-icon';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, AppIcon],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login implements AfterViewInit, OnDestroy {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private authService = inject(AuthService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);
  private host: ElementRef<HTMLElement> = inject(ElementRef);

  isSubmitting = false;
  errorMessage = '';
  autofillLocked = true;
  private autofillCleanupTimers: ReturnType<typeof setTimeout>[] = [];

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  ngAfterViewInit(): void {
    for (const delay of [0, 250, 750]) {
      this.autofillCleanupTimers.push(setTimeout(() => this.clearForcedAutofill(), delay));
    }
  }

  ngOnDestroy(): void {
    this.autofillCleanupTimers.forEach((timer) => clearTimeout(timer));
  }

  unlockAutofill(): void {
    this.autofillLocked = false;
  }

  private clearForcedAutofill(): void {
    if (!this.autofillLocked) {
      return;
    }

    this.loginForm.reset({ email: '', password: '' }, { emitEvent: false });
    const loginInputs = this.host.nativeElement.querySelectorAll(
      'input[data-sbqs-login-field]'
    ) as NodeListOf<HTMLInputElement>;

    loginInputs.forEach((input: HTMLInputElement) => {
        input.value = '';
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

    this.authService.login(payload).pipe(
      timeout(15000),
      finalize(() => {
        this.isSubmitting = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (response) => {
        this.router.navigateByUrl(this.authService.getHomeRoute(response.role));
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Đăng nhập thất bại. Vui lòng kiểm tra email và mật khẩu.'
        );
        this.cdr.detectChanges();
      },
    });
  }
}
