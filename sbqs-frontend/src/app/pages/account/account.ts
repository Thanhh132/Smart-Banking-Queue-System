import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { AccountProfile, AccountService } from '../../core/services/account.service';
import { ApiErrorService } from '../../core/services/api-error.service';
import { AppIcon } from '../../shared/components/app-icon/app-icon';
import { PreventAutofillDirective } from '../../shared/directives/prevent-autofill.directive';
import { DashboardLayout } from '../../shared/layouts/dashboard-layout/dashboard-layout';
import {
  PASSWORD_POLICY_MESSAGE,
  PASSWORD_POLICY_PATTERN,
} from '../../shared/utils/password-policy.util';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DashboardLayout,
    AppIcon,
    PreventAutofillDirective,
  ],
  templateUrl: './account.html',
  styleUrl: './account.scss',
})
export class Account {
  private fb = inject(FormBuilder);
  private accountService = inject(AccountService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  readonly passwordPolicyMessage = PASSWORD_POLICY_MESSAGE;
  profile: AccountProfile | null = null;
  isLoading = true;
  isSavingProfile = false;
  isChangingPassword = false;
  loadError = '';
  profileMessage = '';
  profileError = '';
  passwordMessage = '';
  passwordError = '';

  profileForm = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required, Validators.maxLength(30)]],
  });

  passwordForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.pattern(PASSWORD_POLICY_PATTERN)]],
    confirmPassword: ['', Validators.required],
  });

  constructor() {
    this.loadProfile();
  }

  get roleLabel(): string {
    const labels: Record<string, string> = {
      SUPER_ADMIN: 'Quản trị hệ thống',
      BRANCH_ADMIN: 'Quản trị chi nhánh',
      STAFF: 'Nhân viên',
      CUSTOMER: 'Khách hàng',
    };
    return labels[this.profile?.role || ''] || this.profile?.role || '';
  }

  get canEditProfile(): boolean {
    return this.profile?.role === 'CUSTOMER';
  }

  loadProfile(): void {
    this.isLoading = true;
    this.loadError = '';
    this.accountService.getProfile().pipe(
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }),
    ).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.profileForm.patchValue({
          fullName: profile.fullName,
          email: profile.email,
          phone: profile.phone,
        });
        if (profile.role !== 'CUSTOMER') {
          this.profileForm.disable();
        }
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.loadError = this.apiError.getMessage(error, 'Không tải được thông tin tài khoản.');
        this.cdr.detectChanges();
      },
    });
  }

  /** CUSTOMER gửi yêu cầu sửa hồ sơ; nhân viên không đi vào luồng này. */
  saveProfile(): void {
    this.profileMessage = '';
    this.profileError = '';
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSavingProfile = true;
    this.accountService.requestProfileChange({
      fullName: this.profileForm.value.fullName || '',
      email: this.profileForm.value.email || '',
      phone: this.profileForm.value.phone || '',
    }).pipe(
      finalize(() => {
        this.isSavingProfile = false;
        this.cdr.detectChanges();
      }),
    ).subscribe({
      next: () => {
        this.profileMessage = 'Đã gửi link xác nhận tới email hiện tại. Thông tin chỉ thay đổi sau khi bạn xác nhận.';
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.profileError = this.apiError.getMessage(error, 'Không cập nhật được thông tin.');
        this.cdr.detectChanges();
      },
    });
  }

  /** Kiểm tra form và mật khẩu xác nhận trước khi gọi API đổi mật khẩu. */
  changePassword(): void {
    this.passwordMessage = '';
    this.passwordError = '';
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    const currentPassword = this.passwordForm.value.currentPassword || '';
    const newPassword = this.passwordForm.value.newPassword || '';
    if (newPassword !== this.passwordForm.value.confirmPassword) {
      this.passwordError = 'Mật khẩu xác nhận không khớp.';
      return;
    }

    this.isChangingPassword = true;
    this.accountService.changePassword({ currentPassword, newPassword }).pipe(
      finalize(() => {
        this.isChangingPassword = false;
        this.cdr.detectChanges();
      }),
    ).subscribe({
      next: () => {
        this.passwordForm.reset();
        this.passwordMessage = 'Mật khẩu đã được thay đổi.';
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.passwordError = this.apiError.getMessage(error, 'Không thay đổi được mật khẩu.');
        this.cdr.detectChanges();
      },
    });
  }
}
