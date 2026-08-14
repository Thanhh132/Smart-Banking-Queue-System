import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { PASSWORD_POLICY_PATTERN } from '../../../shared/utils/password-policy.util';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';
import { PreventAutofillDirective } from '../../../shared/directives/prevent-autofill.directive';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, AppIcon, PreventAutofillDirective],
  templateUrl: './register.html',
  styleUrls: ['./register.scss', '../../../../styles/feature/_auth.scss'],
})
export class Register {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  isSubmitting = false;
  errorMessage = '';
  successMessage = '';
  showPassword = false;
  showConfirmPassword = false;

  registerForm = this.fb.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.pattern(PASSWORD_POLICY_PATTERN)]],
    confirmPassword: ['', [Validators.required]],
  });

  submit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      this.cdr.detectChanges();
      return;
    }

    if (this.registerForm.value.password !== this.registerForm.value.confirmPassword) {
      this.errorMessage = 'Mật khẩu xác nhận không khớp.';
      this.cdr.detectChanges();
      return;
    }

    const payload = {
      fullName: this.registerForm.value.fullName || '',
      email: this.registerForm.value.email || '',
      phone: this.registerForm.value.phone || '',
      password: this.registerForm.value.password || '',
      confirmPassword: this.registerForm.value.confirmPassword || '',
    };

    this.isSubmitting = true;

    this.authService
      .register(payload)
      .pipe(
        timeout(15000),
        finalize(() => {
          this.isSubmitting = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: () => {
          this.successMessage = 'Đăng ký thành công. Hãy kiểm tra email để kích hoạt tài khoản.';
          this.registerForm.reset();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.errorMessage = this.apiError.getMessage(
            err,
            'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin.',
          );
          this.cdr.detectChanges();
        },
      });
  }

  togglePasswordVisibility(field: 'password' | 'confirmPassword'): void {
    if (field === 'password') {
      this.showPassword = !this.showPassword;
      return;
    }
    this.showConfirmPassword = !this.showConfirmPassword;
  }
}
