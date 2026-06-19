import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { finalize, timeout, catchError, of } from 'rxjs';
import { ChangeDetectorRef } from '@angular/core';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppCard } from '../../../shared/components/app-card/app-card';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { UserManagementService } from '../../../core/services/user-management.service';


@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DashboardLayout, AppCard],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.scss',
})
export class AdminUsers implements OnInit {
  users: any[] = [];

  staffForm!: FormGroup;

  isLoading = false;
  isListLoading = false;
  isSubmitting = false;
  isEditMode = false;

  editingUserId: number | null = null;
  successMessage = '';
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private userManagementService: UserManagementService,
    private apiError: ApiErrorService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.initForm();
    this.loadUsers();
  }

  initForm(): void {
    this.staffForm = this.fb.group({
      fullName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^[0-9]{10,11}$/)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  loadUsers(): void {
    const branchId = Number(localStorage.getItem('selectedBranchId'));

    if (!branchId) {
      this.errorMessage = 'Chưa chọn chi nhánh.';
      this.users = [];
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.userManagementService.getUsersByBranch(branchId).subscribe({
      next: (res: any) => {
        this.users = Array.isArray(res) ? res : [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Load users error:', err);

        this.users = [];
        this.errorMessage = 'Không thể tải danh sách nhân viên.';
        this.isLoading = false;
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

    const selectedBranchId = Number(localStorage.getItem('selectedBranchId'));

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
      role: 'STAFF',
      branchId: selectedBranchId,
    };

    this.userManagementService.createStaff(payload).subscribe({
      next: () => {
        this.successMessage = 'Tạo tài khoản nhân viên thành công.';
        this.staffForm.reset();
        this.isSubmitting = false;
        this.cdr.detectChanges();

        this.loadUsers();
      },
      error: (err) => {
        console.error('Create staff error:', err);

        this.errorMessage = this.apiError.getMessage(
          err,
          'Tạo tài khoản thất bại. Vui lòng kiểm tra lại dữ liệu.'
        );

        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  deleteUser(user: any): void {
    const confirmed = confirm(
      `Bạn có chắc muốn xóa nhân viên "${user.fullName}" không?`
    );

    if (!confirmed) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.userManagementService.deleteUser(user.userId).subscribe({
      next: () => {
        this.successMessage = 'Xóa nhân viên thành công.';
        this.loadUsers();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Delete user error:', err);

        this.errorMessage = this.apiError.getMessage(
          err,
          'Xóa nhân viên thất bại.'
        );

        this.cdr.detectChanges();
      },
    });
  }

  startEdit(user: any): void {
    this.isEditMode = true;
    this.editingUserId = user.userId;

    this.staffForm.patchValue({
      fullName: user.fullName,
      email: user.email,
      phone: user.phone,
      password: '123456',
    });

    this.cdr.detectChanges();
  }

  cancelEdit(): void {
    this.isEditMode = false;
    this.editingUserId = null;
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
      status: 'ACTIVE'
    };

    this.isSubmitting = true;

    this.userManagementService
      .updateUser(
        this.editingUserId,
        payload
      )
      .subscribe({
        next: () => {

          this.successMessage =
            'Cập nhật nhân viên thành công.';

          this.isSubmitting = false;

          this.isEditMode = false;
          this.editingUserId = null;

          this.staffForm.reset();

          this.loadUsers();

          this.cdr.detectChanges();
        },
        error: (err) => {

          console.error(
            'Update user error:',
            err
          );

          this.errorMessage = this.apiError.getMessage(
            err,
            'Cập nhật thất bại.'
          );

          this.isSubmitting = false;

          this.cdr.detectChanges();
        }
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

      return `${label} không đúng định dạng.`;
    }

    return `${label} không hợp lệ.`;
  }
}
