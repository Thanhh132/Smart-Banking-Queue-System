import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { BranchService } from '../../../core/services/branch.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppButton } from '../../../shared/components/app-button/app-button';
import { AppCard } from '../../../shared/components/app-card/app-card';
import { AppConfirmDialog } from '../../../shared/components/app-confirm-dialog/app-confirm-dialog';
import { AppDataTableShell } from '../../../shared/components/app-data-table-shell/app-data-table-shell';
import { AppModalShell } from '../../../shared/components/app-modal-shell/app-modal-shell';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { ReportExportButtons } from '../../../shared/components/report-export-buttons/report-export-buttons';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';
import { PASSWORD_POLICY_PATTERN } from '../../../shared/utils/password-policy.util';
import { PreventAutofillDirective } from '../../../shared/directives/prevent-autofill.directive';

@Component({
  selector: 'app-super-admin',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    DashboardLayout,
    AppButton,
    AppCard,
    AppConfirmDialog,
    AppDataTableShell,
    ReportExportButtons,
    AppIcon,
    AppModalShell,
    AppPageHeader,
    PreventAutofillDirective,
  ],
  templateUrl: './super-admin.html',
  styleUrl: './super-admin.scss',
})
export class SuperAdmin implements OnInit {
  private fb = inject(FormBuilder);
  private branchService = inject(BranchService);
  private userService = inject(UserManagementService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  branches: any[] = [];
  adminBranches: any[] = [];
  isListLoading = false;
  isSubmitting = false;
  editingAdminId: number | null = null;
  successMessage = '';
  errorMessage = '';
  searchTerm = '';
  showPassword = false;
  showConfirmPassword = false;
  isAdminModalOpen = false;
  pendingDeleteAdmin: any | null = null;
  isDeleting = false;

  get filteredAdmins(): any[] {
    const keyword = this.searchTerm.trim().toLocaleLowerCase('vi');
    if (!keyword) return this.adminBranches;
    return this.adminBranches.filter((admin) =>
      [admin.fullName, admin.email, admin.phone, admin.branch?.branchName]
        .filter(Boolean)
        .some((value) => String(value).toLocaleLowerCase('vi').includes(keyword)),
    );
  }

  adminBranchForm = this.fb.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.pattern(PASSWORD_POLICY_PATTERN)]],
    confirmPassword: ['', [Validators.required]],
    branchId: [null as number | null, [Validators.required]],
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
          'Không tải được danh sách quản trị chi nhánh.',
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

    if (this.adminBranchForm.value.password !== this.adminBranchForm.value.confirmPassword) {
      this.errorMessage = 'Mật khẩu xác nhận không khớp.';
      this.cdr.detectChanges();
      return;
    }

    this.isSubmitting = true;

    const payload = {
      fullName: this.adminBranchForm.value.fullName,
      email: this.adminBranchForm.value.email,
      phone: this.adminBranchForm.value.phone,
      password: this.adminBranchForm.value.password,
      confirmPassword: this.adminBranchForm.value.confirmPassword,
      branchId: this.adminBranchForm.value.branchId,
    };

    this.userService.createAdminBranch(payload).subscribe({
      next: () => {
        this.successMessage = 'Đã tạo tài khoản quản trị chi nhánh.';
        this.isSubmitting = false;
        this.resetAdminBranchForm();
        this.isAdminModalOpen = false;
        this.loadAdminBranches();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Tạo tài khoản quản trị chi nhánh thất bại.',
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
    const confirmPasswordControl = this.adminBranchForm.get('confirmPassword');
    passwordControl?.clearValidators();
    passwordControl?.updateValueAndValidity();
    confirmPasswordControl?.clearValidators();
    confirmPasswordControl?.updateValueAndValidity();

    this.adminBranchForm.reset({
      fullName: admin.fullName || '',
      email: admin.email || '',
      phone: admin.phone || '',
      password: '',
      confirmPassword: '',
      branchId: admin.branch?.branchId || null,
    });
    this.isAdminModalOpen = true;
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
    };

    this.userService.updateUser(this.editingAdminId, payload).subscribe({
      next: () => {
        this.successMessage = 'Đã cập nhật tài khoản quản trị chi nhánh.';
        this.isSubmitting = false;
        this.resetAdminBranchForm();
        this.isAdminModalOpen = false;
        this.loadAdminBranches();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Cập nhật tài khoản quản trị chi nhánh thất bại.',
        );
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  cancelEditAdminBranch(): void {
    if (this.isSubmitting) return;
    this.resetAdminBranchForm();
    this.isAdminModalOpen = false;
    this.successMessage = '';
    this.errorMessage = '';
    this.cdr.detectChanges();
  }

  private resetAdminBranchForm(): void {
    this.editingAdminId = null;

    const passwordControl = this.adminBranchForm.get('password');
    const confirmPasswordControl = this.adminBranchForm.get('confirmPassword');
    passwordControl?.setValidators([
      Validators.required,
      Validators.pattern(PASSWORD_POLICY_PATTERN),
    ]);
    passwordControl?.updateValueAndValidity();
    confirmPasswordControl?.setValidators([Validators.required]);
    confirmPasswordControl?.updateValueAndValidity();

    this.adminBranchForm.reset({
      fullName: '',
      email: '',
      phone: '',
      password: '',
      confirmPassword: '',
      branchId: null,
    });
  }

  deleteAdminBranch(user: any): void {
    if (this.isDeleting) return;
    this.pendingDeleteAdmin = user;
  }

  confirmDeleteAdminBranch(): void {
    const user = this.pendingDeleteAdmin;
    if (!user || this.isDeleting) return;

    this.successMessage = '';
    this.errorMessage = '';
    this.isDeleting = true;

    this.userService.deleteUser(user.userId).subscribe({
      next: () => {
        this.successMessage = 'Đã xóa vĩnh viễn tài khoản quản trị chi nhánh.';
        this.isDeleting = false;
        this.pendingDeleteAdmin = null;
        this.loadAdminBranches();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không xóa được tài khoản quản trị chi nhánh.',
        );
        this.isDeleting = false;
        this.cdr.detectChanges();
      },
    });
  }

  cancelDeleteAdminBranch(): void {
    if (!this.isDeleting) {
      this.pendingDeleteAdmin = null;
    }
  }

  openCreateAdminBranch(): void {
    this.resetAdminBranchForm();
    this.successMessage = '';
    this.errorMessage = '';
    this.isAdminModalOpen = true;
  }

  navigateToBranches(): void {
    void this.router.navigate(['/super-admin/branches']);
  }

  togglePasswordVisibility(field: 'password' | 'confirmPassword'): void {
    if (field === 'password') {
      this.showPassword = !this.showPassword;
      return;
    }
    this.showConfirmPassword = !this.showConfirmPassword;
  }
}
