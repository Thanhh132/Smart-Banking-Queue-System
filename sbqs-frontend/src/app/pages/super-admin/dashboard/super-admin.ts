import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Branch } from '../../../core/models/branch.model';
import { ServiceCatalogItem } from '../../../core/models/service.model';
import { AdminServicesService } from '../../../core/services/admin-services.service';
import { BranchService } from '../../../core/services/branch.service';
import { ManagedUser, UserManagementService } from '../../../core/services/user-management.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppButton } from '../../../shared/components/app-button/app-button';
import { AppCard } from '../../../shared/components/app-card/app-card';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { ReportExportButtons } from '../../../shared/components/report-export-buttons/report-export-buttons';

@Component({
  selector: 'app-super-admin',
  standalone: true,
  imports: [CommonModule, DashboardLayout, AppButton, AppCard, AppPageHeader, ReportExportButtons],
  templateUrl: './super-admin.html',
  styleUrl: './super-admin.scss',
})
export class SuperAdmin implements OnInit {
  private branchService = inject(BranchService);
  private userService = inject(UserManagementService);
  private serviceApi = inject(AdminServicesService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  branches: Branch[] = [];
  branchAdmins: ManagedUser[] = [];
  staff: ManagedUser[] = [];
  catalog: ServiceCatalogItem[] = [];
  isLoading = true;
  errorMessage = '';

  get activeBranches(): number {
    return this.branches.filter((branch) => branch.status === 'ACTIVE').length;
  }

  get activeCatalog(): number {
    return this.catalog.filter((item) => item.status === 'ACTIVE').length;
  }

  get branchesWithoutAdmin(): number {
    const assigned = new Set(this.branchAdmins.map((user) => user.branch?.branchId).filter(Boolean));
    return this.branches.filter((branch) => !assigned.has(branch.branchId)).length;
  }

  ngOnInit(): void {
    this.loadOverview();
  }

  loadOverview(): void {
    this.isLoading = true;
    this.errorMessage = '';
    forkJoin({
      branches: this.branchService.getBranches(),
      branchAdmins: this.userService.getUsersByRole('BRANCH_ADMIN'),
      staff: this.userService.getUsersByRole('STAFF'),
      catalog: this.serviceApi.getCatalog(),
    }).subscribe({
      next: ({ branches, branchAdmins, staff, catalog }) => {
        this.branches = branches || [];
        this.branchAdmins = branchAdmins || [];
        this.staff = staff || [];
        this.catalog = catalog || [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Không tải được số liệu tổng quan hệ thống.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  navigateToBranches(): void {
    void this.router.navigate(['/super-admin/branches']);
  }

  navigateToCatalog(): void {
    void this.router.navigate(['/super-admin/services']);
  }
}
