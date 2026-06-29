import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { PASSWORD_POLICY_PATTERN } from '../../../shared/utils/password-policy.util';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, AppIcon],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private authService = inject(AuthService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  isSubmitting = false;
  errorMessage = '';

  registerForm = this.fb.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.pattern(PASSWORD_POLICY_PATTERN)]],
  });

  submit(): void {
    this.errorMessage = '';

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      this.cdr.detectChanges();
      return;
    }

    const payload = {
      fullName: this.registerForm.value.fullName || '',
      email: this.registerForm.value.email || '',
      phone: this.registerForm.value.phone || '',
      password: this.registerForm.value.password || '',
    };

    this.isSubmitting = true;

    this.authService.register(payload).pipe(
      timeout(15000),
      finalize(() => {
        this.isSubmitting = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: () => {
        this.router.navigateByUrl('/login');
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin.'
        );
        this.cdr.detectChanges();
      },
    });
  }
}
