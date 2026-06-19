import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { BranchService } from '../../../core/services/branch.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({
  selector: 'app-super-admin',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DashboardLayout],
  templateUrl: './super-admin.html',
  styleUrl: './super-admin.scss',
})
export class SuperAdmin implements OnInit {
  private fb = inject(FormBuilder);
  private branchService = inject(BranchService);
  private userService = inject(UserManagementService);

  branches: any[] = [];
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
    this.branchService.getBranches().subscribe({
      next: (branches) => {
        this.branches = branches;
      },
      error: () => {
        this.errorMessage = 'Khong tai duoc danh sach chi nhanh.';
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
        this.adminBranchForm.reset();
      },
      error: (err) => {
        this.errorMessage =
          err?.error?.message || err?.error || 'Tao admin chi nhanh that bai.';
        this.isSubmitting = false;
      },
    });
  }
}
