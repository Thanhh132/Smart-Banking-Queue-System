import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';
import { PASSWORD_POLICY_PATTERN } from '../../../shared/utils/password-policy.util';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';
import { PreventAutofillDirective } from '../../../shared/directives/prevent-autofill.directive';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, AppIcon, PreventAutofillDirective],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.scss',
})
export class ResetPassword {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  token = this.route.snapshot.queryParamMap.get('token') || '';
  isSubmitting = false;
  isCompleted = false;
  errorMessage = this.token ? '' : 'Liên kết đặt lại mật khẩu không hợp lệ.';
  form = this.fb.group({
    password: ['', [Validators.required, Validators.pattern(PASSWORD_POLICY_PATTERN)]],
    confirmPassword: ['', [Validators.required]],
  });

  submit(): void {
    this.errorMessage = '';
    if (!this.token || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const password = this.form.value.password || '';
    if (password !== this.form.value.confirmPassword) {
      this.errorMessage = 'Mật khẩu xác nhận không khớp.';
      return;
    }

    this.isSubmitting = true;
    this.authService
      .resetPassword(this.token, password)
      .pipe(
        finalize(() => {
          this.isSubmitting = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: () => {
          this.isCompleted = true;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.errorMessage = this.apiError.getMessage(err, 'Không đặt lại được mật khẩu.');
          this.cdr.detectChanges();
        },
      });
  }
}
