import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { BranchService } from '../../../core/services/branch.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { ReportExportButtons } from '../../../shared/components/report-export-buttons/report-export-buttons';
import { PASSWORD_POLICY_PATTERN } from '../../../shared/utils/password-policy.util';

@Component({
  selector: 'app-super-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink, DashboardLayout, ReportExportButtons],
  templateUrl: './super-admin.html',
  styleUrl: './super-admin.scss',
})
export class SuperAdmin implements OnInit {
  private fb = inject(FormBuilder);
  private branchService = inject(BranchService);
  private userService = inject(UserManagementService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  branches: any[] = [];
  adminBranches: any[] = [];
  isListLoading = false;
  isSubmitting = false;
  editingAdminId: number | null = null;
  successMessage = '';
  errorMessage = '';
  searchTerm = '';

  get activeAdminCount(): number {
    return this.adminBranches.filter((admin) => admin.status === 'ACTIVE').length;
  }

  get filteredAdmins(): any[] {
    const keyword = this.searchTerm.trim().toLocaleLowerCase('vi');
    if (!keyword) return this.adminBranches;
    return this.adminBranches.filter((admin) =>
      [admin.fullName, admin.email, admin.phone, admin.branch?.branchName]
        .filter(Boolean)
        .some((value) => String(value).toLocaleLowerCase('vi').includes(keyword))
    );
  }

  adminBranchForm = this.fb.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.pattern(PASSWORD_POLICY_PATTERN)]],
    branchId: [null as number | null, [Validators.required]],
    status: ['ACTIVE'],
  });

  get isEditMode(): boolean {
    return this.editingAdminId !== null;
  }

  ngOnInit(): void {
    this.loadPageData();
  }

  loadPageData(): void {
    this.loadBranches();
    this.loadAdminBranches();
  }

  loadBranches(): void {
    this.branchService.getBranches().subscribe({
      next: (branches) => {
        this.branches = branches;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Không tải được danh sách chi nhánh.';
        this.cdr.detectChanges();
      },
    });
  }

  loadAdminBranches(): void {
    this.isListLoading = true;
    this.userService.getUsersByRole('BRANCH_ADMIN').subscribe({
      next: (users) => {
        this.adminBranches = users || [];
        this.isListLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không tải được danh sách quản trị chi nhánh.'
        );
        this.isListLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  submitAdminBranch(): void {
    if (this.isEditMode) {
      this.updateAdminBranch();
      return;
    }

    this.createAdminBranch();
  }

  createAdminBranch(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.adminBranchForm.invalid) {
      this.adminBranchForm.markAllAsTouched();
      this.cdr.detectChanges();
      return;
    }

    this.isSubmitting = true;

    const payload = {
      fullName: this.adminBranchForm.value.fullName,
      email: this.adminBranchForm.value.email,
      phone: this.adminBranchForm.value.phone,
      password: this.adminBranchForm.value.password,
      branchId: this.adminBranchForm.value.branchId,
    };

    this.userService.createAdminBranch(payload).subscribe({
      next: () => {
        this.successMessage = 'Đã tạo tài khoản quản trị chi nhánh.';
        this.isSubmitting = false;
        this.resetAdminBranchForm();
        this.loadAdminBranches();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Tạo tài khoản quản trị chi nhánh thất bại.'
        );
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  startEditAdminBranch(admin: any): void {
    this.editingAdminId = admin.userId;
    this.successMessage = '';
    this.errorMessage = '';

    const passwordControl = this.adminBranchForm.get('password');
    passwordControl?.clearValidators();
    passwordControl?.updateValueAndValidity();

    this.adminBranchForm.reset({
      fullName: admin.fullName || '',
      email: admin.email || '',
      phone: admin.phone || '',
      password: '',
      branchId: admin.branch?.branchId || null,
      status: admin.status || 'ACTIVE',
    });
    this.cdr.detectChanges();
  }

  updateAdminBranch(): void {
    if (!this.editingAdminId) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    if (this.adminBranchForm.invalid) {
      this.adminBranchForm.markAllAsTouched();
      this.cdr.detectChanges();
      return;
    }

    this.isSubmitting = true;

    const payload = {
      fullName: this.adminBranchForm.value.fullName,
      email: this.adminBranchForm.value.email,
      phone: this.adminBranchForm.value.phone,
      branchId: this.adminBranchForm.value.branchId,
      status: this.adminBranchForm.value.status,
    };

    this.userService.updateUser(this.editingAdminId, payload).subscribe({
      next: () => {
        this.successMessage = 'Đã cập nhật tài khoản quản trị chi nhánh.';
        this.isSubmitting = false;
        this.resetAdminBranchForm();
        this.loadAdminBranches();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Cập nhật tài khoản quản trị chi nhánh thất bại.'
        );
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  cancelEditAdminBranch(): void {
    this.resetAdminBranchForm();
    this.successMessage = '';
    this.errorMessage = '';
    this.cdr.detectChanges();
  }

  private resetAdminBranchForm(): void {
    this.editingAdminId = null;

    const passwordControl = this.adminBranchForm.get('password');
    passwordControl?.setValidators([Validators.required, Validators.pattern(PASSWORD_POLICY_PATTERN)]);
    passwordControl?.updateValueAndValidity();

    this.adminBranchForm.reset({
      fullName: '',
      email: '',
      phone: '',
      password: '',
      branchId: null,
      status: 'ACTIVE',
    });
  }

  deleteAdminBranch(user: any): void {
    const isInactive = user.status === 'INACTIVE';
    const action = isInactive ? 'xóa khỏi danh sách' : 'khóa tài khoản';
    if (!confirm(`Bạn có chắc muốn ${action} quản trị chi nhánh "${user.fullName}" không?`)) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.userService.deleteUser(user.userId).subscribe({
      next: () => {
        this.successMessage = isInactive
          ? 'Đã xóa tài khoản quản trị chi nhánh khỏi danh sách.'
          : 'Đã khóa tài khoản quản trị chi nhánh.';
        this.loadAdminBranches();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          isInactive
            ? 'Không xóa được tài khoản quản trị chi nhánh.'
            : 'Không khóa được tài khoản quản trị chi nhánh.'
        );
        this.cdr.detectChanges();
      },
    });
  }
}
