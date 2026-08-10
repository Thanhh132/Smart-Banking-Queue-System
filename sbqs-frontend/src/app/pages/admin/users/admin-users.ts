import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { AppCard } from '../../../shared/components/app-card/app-card';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { ExcelImportPanel } from '../../../shared/components/excel-import-panel/excel-import-panel';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';
import { PreventAutofillDirective } from '../../../shared/directives/prevent-autofill.directive';
import {
  PASSWORD_POLICY_MESSAGE,
  PASSWORD_POLICY_PATTERN,
} from '../../../shared/utils/password-policy.util';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DashboardLayout,
    AppCard,
    ExcelImportPanel,
    AppIcon,
    PreventAutofillDirective,
  ],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.scss',
})
export class AdminUsers implements OnInit {
  users: any[] = [];
  staffForm!: FormGroup;

  isListLoading = false;
  isSubmitting = false;
  isEditMode = false;
  showPassword = false;
  showConfirmPassword = false;

  editingUserId: number | null = null;
  successMessage = '';
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private userManagementService: UserManagementService,
    private apiError: ApiErrorService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadUsers();
  }

  initForm(): void {
    this.staffForm = this.fb.group({
      fullName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^[0-9]{10,11}$/)]],
      password: ['', [Validators.required, Validators.pattern(PASSWORD_POLICY_PATTERN)]],
      confirmPassword: ['', [Validators.required]],
    });
  }

  loadUsers(): void {
    const branchId = Number(sessionStorage.getItem('selectedBranchId'));

    if (!branchId) {
      this.errorMessage = 'Chưa chọn chi nhánh.';
      this.users = [];
      this.isListLoading = false;
      this.cdr.detectChanges();
      return;
    }

    this.isListLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.userManagementService.getUsersByRole('STAFF').subscribe({
      next: (res: any) => {
        this.users = Array.isArray(res) ? res : [];
        this.isListLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.users = [];
        this.errorMessage = this.apiError.getMessage(err, 'Không thể tải danh sách nhân viên.');
        this.isListLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  createStaff(): void {
    if (this.isEditMode) {
      this.updateUser();
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    if (this.staffForm.invalid) {
      this.staffForm.markAllAsTouched();
      return;
    }

    if (this.staffForm.value.password !== this.staffForm.value.confirmPassword) {
      this.errorMessage = 'Mật khẩu xác nhận không khớp.';
      return;
    }

    const selectedBranchId = Number(sessionStorage.getItem('selectedBranchId'));

    if (!selectedBranchId) {
      this.errorMessage = 'Chưa chọn chi nhánh.';
      return;
    }

    this.isSubmitting = true;

    const payload = {
      fullName: this.staffForm.value.fullName,
      email: this.staffForm.value.email,
      phone: this.staffForm.value.phone,
      password: this.staffForm.value.password,
      confirmPassword: this.staffForm.value.confirmPassword,
      role: 'STAFF',
      branchId: selectedBranchId,
    };

    this.userManagementService.createStaff(payload).subscribe({
      next: () => {
        this.successMessage = 'Tạo tài khoản nhân viên thành công.';
        this.staffForm.reset();
        this.isSubmitting = false;
        this.loadUsers();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Tạo tài khoản thất bại. Vui lòng kiểm tra lại dữ liệu.',
        );
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  deleteUser(user: any): void {
    const confirmed = confirm(
      `Xóa vĩnh viễn nhân viên "${user.fullName}" khỏi SBQS và Keycloak? Thao tác này không thể hoàn tác.`,
    );

    if (!confirmed) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.userManagementService.deleteUser(user.userId).subscribe({
      next: () => {
        this.successMessage = 'Đã xóa vĩnh viễn tài khoản nhân viên.';
        this.users = this.users.filter((item) => item.userId !== user.userId);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Xóa tài khoản nhân viên thất bại.',
        );
        this.cdr.detectChanges();
      },
    });
  }

  startEdit(user: any): void {
    this.isEditMode = true;
    this.editingUserId = user.userId;
    const passwordControl = this.staffForm.get('password');
    const confirmPasswordControl = this.staffForm.get('confirmPassword');
    passwordControl?.clearValidators();
    passwordControl?.updateValueAndValidity();
    confirmPasswordControl?.clearValidators();
    confirmPasswordControl?.updateValueAndValidity();

    this.staffForm.patchValue({
      fullName: user.fullName,
      email: user.email,
      phone: user.phone,
      password: '',
      confirmPassword: '',
    });

    this.cdr.detectChanges();
  }

  cancelEdit(): void {
    this.isEditMode = false;
    this.editingUserId = null;
    const passwordControl = this.staffForm.get('password');
    const confirmPasswordControl = this.staffForm.get('confirmPassword');
    passwordControl?.setValidators([
      Validators.required,
      Validators.pattern(PASSWORD_POLICY_PATTERN),
    ]);
    passwordControl?.updateValueAndValidity();
    confirmPasswordControl?.setValidators([Validators.required]);
    confirmPasswordControl?.updateValueAndValidity();
    this.staffForm.reset();
    this.successMessage = '';
    this.errorMessage = '';
    this.cdr.detectChanges();
  }

  updateUser(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (!this.editingUserId) {
      this.errorMessage = 'Không tìm thấy người dùng.';
      return;
    }

    const payload = {
      fullName: this.staffForm.value.fullName,
      email: this.staffForm.value.email,
      phone: this.staffForm.value.phone,
    };

    this.isSubmitting = true;

    this.userManagementService.updateUser(this.editingUserId, payload).subscribe({
      next: () => {
        this.successMessage = 'Cập nhật nhân viên thành công.';
        this.isSubmitting = false;
        this.isEditMode = false;
        this.editingUserId = null;
        const passwordControl = this.staffForm.get('password');
        const confirmPasswordControl = this.staffForm.get('confirmPassword');
        passwordControl?.setValidators([
          Validators.required,
          Validators.pattern(PASSWORD_POLICY_PATTERN),
        ]);
        passwordControl?.updateValueAndValidity();
        confirmPasswordControl?.setValidators([Validators.required]);
        confirmPasswordControl?.updateValueAndValidity();
        this.staffForm.reset();
        this.loadUsers();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Cập nhật thất bại.');
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  isInvalid(controlName: string): boolean {
    const control = this.staffForm.get(controlName);
    return !!(control && control.touched && control.invalid);
  }

  getErrorMessage(controlName: string, label: string): string {
    const control = this.staffForm.get(controlName);

    if (!control || !control.errors) {
      return '';
    }

    if (control.errors['required']) {
      return `${label} không được để trống.`;
    }

    if (control.errors['email']) {
      return `${label} không đúng định dạng.`;
    }

    if (control.errors['minlength']) {
      return `${label} phải có ít nhất ${control.errors['minlength'].requiredLength} ký tự.`;
    }

    if (control.errors['pattern']) {
      if (controlName === 'phone') {
        return `${label} chỉ gồm số và phải có 10-11 chữ số.`;
      }

      if (controlName === 'password') {
        return PASSWORD_POLICY_MESSAGE;
      }

      return `${label} không đúng định dạng.`;
    }

    return `${label} không hợp lệ.`;
  }

  togglePasswordVisibility(field: 'password' | 'confirmPassword'): void {
    if (field === 'password') {
      this.showPassword = !this.showPassword;
      return;
    }
    this.showConfirmPassword = !this.showConfirmPassword;
  }
}
