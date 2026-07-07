import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs';

import { AccountService } from '../../core/services/account.service';
import { ApiErrorService } from '../../core/services/api-error.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-confirm-account-change',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './confirm-account-change.html',
  styleUrl: './confirm-account-change.scss',
})
export class ConfirmAccountChange {
  private route = inject(ActivatedRoute);
  private accountService = inject(AccountService);
  private authService = inject(AuthService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  isLoading = true;
  isSuccess = false;
  needsNewEmailConfirmation = false;
  emailChanged = false;
  message = 'Đang xác nhận yêu cầu thay đổi tài khoản...';

  constructor() {
    // Trang được mở trực tiếp từ email nên token được lấy từ query string thay vì JWT.
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.isLoading = false;
      this.message = 'Liên kết xác nhận không hợp lệ.';
      return;
    }

    this.accountService.confirmProfileChange(token).pipe(
      timeout(10000),
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }),
    ).subscribe({
      next: (response) => {
        this.isSuccess = true;
        this.needsNewEmailConfirmation = response.status === 'PENDING_NEW_EMAIL';
        this.emailChanged = response.emailChanged;
        this.message = this.needsNewEmailConfirmation
          ? 'Email hiện tại đã xác nhận. Hãy mở MailHog hoặc hộp thư email mới để bấm link xác minh thứ hai.'
          : 'Thông tin tài khoản đã được cập nhật thành công.';
        if (response.emailChanged) {
          this.authService.clearLocalSession();
        }
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.message = this.apiError.getMessage(error, 'Không xác nhận được yêu cầu thay đổi.');
        this.cdr.detectChanges();
      },
    });
  }
}
