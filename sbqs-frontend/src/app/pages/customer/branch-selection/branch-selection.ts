import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { Branch } from '../../../core/models/branch.model';
import { BranchService } from '../../../core/services/branch.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({
  selector: 'app-branch-selection',
  imports: [CommonModule, FormsModule, DashboardLayout],
  templateUrl: './branch-selection.html',
  styleUrl: './branch-selection.scss',
})
export class BranchSelection implements OnInit {
  private branchService = inject(BranchService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  branches: Branch[] = [];
  errorMessage = '';
  searchTerm = '';

  get filteredBranches(): Branch[] {
    const keyword = this.searchTerm.trim().toLocaleLowerCase('vi');
    if (!keyword) {
      return this.branches;
    }

    return this.branches.filter((branch) =>
      [branch.bankName, branch.branchName, branch.district, branch.province, branch.address]
        .filter(Boolean)
        .some((value) => String(value).toLocaleLowerCase('vi').includes(keyword))
    );
  }

  ngOnInit(): void {
    this.branchService.getBranches().subscribe({
      next: (data) => {
        this.branches = data || [];
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Không tải được danh sách chi nhánh.';
        this.cdr.detectChanges();
      },
    });
  }

  selectBranch(branchId: number): void {
    localStorage.setItem('selectedBranchId', String(branchId));
    this.router.navigate(['/services']);
  }
}
