import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { timeout } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';
import { PreventAutofillDirective } from '../../../shared/directives/prevent-autofill.directive';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, AppIcon, PreventAutofillDirective],
  templateUrl: './forgot-password.html',
  styleUrls: ['./forgot-password.scss', '../../../../styles/feature/_auth.scss'],
})
export class ForgotPassword {
  private static readonly COOLDOWN_MS = 5 * 60 * 1000;

  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);

  isSubmitting = false;
  isSubmitted = false;
  noticeMessage = '';
  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  submit(): void {
    this.noticeMessage = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const email = (this.form.value.email || '').trim().toLowerCase();
    const remainingMs = this.getRemainingCooldown(email);

    if (remainingMs > 0) {
      this.noticeMessage = `Bạn vừa yêu cầu đặt lại mật khẩu cho email này. Vui lòng kiểm tra hộp thư hoặc thử lại sau ${this.formatRemainingTime(remainingMs)}.`;
      this.cdr.detectChanges();
      return;
    }

    this.saveCooldown(email);
    this.isSubmitting = false;
    this.isSubmitted = true;
    this.cdr.detectChanges();

    this.authService
      .forgotPassword(email)
      .pipe(timeout(15000))
      .subscribe({
        next: () => {},
        error: () => {},
      });
  }

  private getRemainingCooldown(email: string): number {
    const value = localStorage.getItem(this.cooldownKey(email));
    const lastRequestedAt = Number(value || 0);

    if (!lastRequestedAt) {
      return 0;
    }

    return Math.max(0, ForgotPassword.COOLDOWN_MS - (Date.now() - lastRequestedAt));
  }

  private saveCooldown(email: string): void {
    localStorage.setItem(this.cooldownKey(email), String(Date.now()));
  }

  private cooldownKey(email: string): string {
    return `sbqs:forgot-password:${email}`;
  }

  private formatRemainingTime(milliseconds: number): string {
    const totalSeconds = Math.ceil(milliseconds / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;

    if (minutes <= 0) {
      return `${seconds} giây`;
    }

    return seconds === 0 ? `${minutes} phút` : `${minutes} phút ${seconds} giây`;
  }
}
