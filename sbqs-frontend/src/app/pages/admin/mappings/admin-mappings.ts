import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { AppCard } from '../../../shared/components/app-card/app-card';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AdminMappingsService } from '../../../core/services/admin-mappings.service';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';

@Component({
  selector: 'app-admin-mappings',
  imports: [
    CommonModule,
    FormsModule,
    AppCard,
    DashboardLayout,
    AppPageHeader,
    AppIcon,
  ],
  templateUrl: './admin-mappings.html',
  styleUrl: './admin-mappings.scss',
})
export class AdminMappings implements OnInit {
  private mappingService = inject(AdminMappingsService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  branchId = Number(localStorage.getItem('selectedBranchId')) || null;
  queueMachines: any[] = [];
  services: any[] = [];
  mappings: any[] = [];
  successMessage = '';
  errorMessage = '';

  selectedQueueMachineId: number | null = null;
  selectedServiceIds: number[] = [];

  ngOnInit(): void {
    if (!this.ensureBranch()) {
      return;
    }

    this.loadData();
  }

  loadData(): void {
    if (!this.ensureBranch()) {
      return;
    }

    this.mappingService.getQueueMachines().subscribe({
      next: (data) => {
        this.queueMachines = (data || []).filter(
          (machine) => machine.branch?.branchId === this.branchId
        );

        if (!this.selectedQueueMachineId && this.queueMachines.length > 0) {
          this.selectedQueueMachineId = this.queueMachines[0].queueMachineId;
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không tải được máy bốc số.');
        this.cdr.detectChanges();
      },
    });

    this.mappingService.getServices(this.branchId).subscribe({
      next: (data) => {
        this.services = data || [];
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không tải được dịch vụ.');
        this.cdr.detectChanges();
      },
    });

    this.loadMappings();
  }

  loadMappings(): void {
    this.mappingService.getMappings().subscribe({
      next: (data) => {
        this.mappings = (data || []).filter(
          (mapping) => mapping.queueMachine?.branch?.branchId === this.branchId
        );
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không tải được danh sách liên kết.');
        this.cdr.detectChanges();
      },
    });
  }

  toggleService(serviceId: number, checked: boolean): void {
    if (checked) {
      this.selectedServiceIds = Array.from(new Set([...this.selectedServiceIds, serviceId]));
      return;
    }

    this.selectedServiceIds = this.selectedServiceIds.filter((id) => id !== serviceId);
  }

  createMappings(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (!this.selectedQueueMachineId || this.selectedServiceIds.length === 0) {
      this.errorMessage = 'Hãy chọn máy bốc số và ít nhất một dịch vụ.';
      this.cdr.detectChanges();
      return;
    }

    forkJoin(
      this.selectedServiceIds.map((serviceId) =>
        this.mappingService.createMapping(Number(this.selectedQueueMachineId), serviceId)
      )
    ).subscribe({
      next: () => {
        this.successMessage = `Đã tạo ${this.selectedServiceIds.length} liên kết.`;
        this.selectedServiceIds = [];
        this.loadMappings();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không tạo được liên kết. Có thể một liên kết đã tồn tại.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  deleteMapping(mapping: any): void {
    if (!confirm('Gỡ liên kết này?')) {
      return;
    }

    const queueMachineId = mapping.queueMachine?.queueMachineId || mapping.id?.queueMachineId;
    const serviceId = mapping.service?.serviceId || mapping.id?.serviceId;

    if (!queueMachineId || !serviceId) {
      this.errorMessage = 'Không xác định được liên kết cần xóa.';
      this.cdr.detectChanges();
      return;
    }

    this.mappingService.deleteMapping(queueMachineId, serviceId).subscribe({
      next: () => {
        this.successMessage = 'Đã gỡ liên kết.';
        this.loadMappings();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Gỡ liên kết thất bại.');
        this.cdr.detectChanges();
      },
    });
  }

  private ensureBranch(): this is this & { branchId: number } {
    if (!this.branchId) {
      this.errorMessage =
        'Tài khoản Branch Admin này chưa được gán chi nhánh. Hãy dùng tài khoản do Super Admin cấp cho chi nhánh.';
      this.cdr.detectChanges();
      return false;
    }

    return true;
  }
}
