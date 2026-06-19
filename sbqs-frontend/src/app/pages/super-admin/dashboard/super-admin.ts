import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { BranchService } from '../../../core/services/branch.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({
  selector: 'app-super-admin',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, DashboardLayout],
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

  adminBranchForm = this.fb.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(6)]],
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
        this.errorMessage = 'Khong tai duoc danh sach chi nhanh.';
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
          'Khong tai duoc danh sach admin branch.'
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
      return;
    }

    this.isSubmitting = true;

    this.userService.createAdminBranch(this.adminBranchForm.value).subscribe({
      next: () => {
        this.successMessage = 'Da tao tai khoan admin chi nhanh.';
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
          'Tao admin chi nhanh that bai.'
        );
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  deleteAdminBranch(user: any): void {
    if (!confirm(`Xoa han admin branch "${user.fullName}"?`)) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.userService.deleteUser(user.userId).subscribe({
      next: () => {
        this.successMessage = 'Da xoa admin branch.';
        this.loadAdminBranches();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Khong xoa duoc admin branch.'
        );
        this.cdr.detectChanges();
      },
    });
  }
}
