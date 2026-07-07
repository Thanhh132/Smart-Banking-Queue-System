import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { finalize, timeout } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { ApiErrorService } from '../../../core/services/api-error.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './verify-email.html',
  styleUrl: './verify-email.scss',
})
export class VerifyEmail implements OnInit {
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  isLoading = true;
  isVerified = false;
  message = 'Đang xác minh địa chỉ email...';
  email = '';
  resendMessage = '';

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.isLoading = false;
      this.message = 'Liên kết xác minh không hợp lệ.';
      this.cdr.detectChanges();
      return;
    }

    this.authService.verifyEmail(token).pipe(
      timeout(10000),
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }),
    ).subscribe({
      next: () => {
        this.isVerified = true;
        this.message = 'Email đã được xác minh. Bạn có thể đăng nhập.';
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.message = this.apiError.getMessage(error, 'Không thể xác minh email.');
        this.cdr.detectChanges();
      },
    });
  }

  resend(): void {
    if (!this.email.trim()) {
      this.resendMessage = 'Vui lòng nhập email.';
      this.cdr.detectChanges();
      return;
    }
    this.authService.resendVerification(this.email.trim()).subscribe({
      next: () => {
        this.resendMessage = 'Nếu tài khoản còn chờ xác minh, email mới đã được gửi. Nếu đã xác minh trước đó, hãy thử đăng nhập.';
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.resendMessage = this.apiError.getMessage(error, 'Không thể gửi lại email.');
        this.cdr.detectChanges();
      },
    });
  }
}
