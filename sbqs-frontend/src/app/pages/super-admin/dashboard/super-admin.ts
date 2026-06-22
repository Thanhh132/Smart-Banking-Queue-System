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
    password: ['', [Validators.required, Validators.minLength(8)]],
    branchId: [null as number | null, [Validators.required]],
  });

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

  createAdminBranch(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.adminBranchForm.invalid) {
      this.adminBranchForm.markAllAsTouched();
      this.cdr.detectChanges();
      return;
    }

    this.isSubmitting = true;

    this.userService.createAdminBranch(this.adminBranchForm.value).subscribe({
      next: () => {
        this.successMessage = 'Đã tạo tài khoản quản trị chi nhánh.';
        this.isSubmitting = false;
        this.adminBranchForm.reset({
          fullName: '',
          email: '',
          phone: '',
          password: '',
          branchId: null,
        });
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

  deleteAdminBranch(user: any): void {
    if (!confirm(`Xóa vĩnh viễn tài khoản "${user.fullName}"?`)) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.userService.deleteUser(user.userId).subscribe({
      next: () => {
        this.successMessage = 'Đã xóa tài khoản quản trị chi nhánh.';
        this.loadAdminBranches();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không xóa được tài khoản quản trị chi nhánh.'
        );
        this.cdr.detectChanges();
      },
    });
  }
}
