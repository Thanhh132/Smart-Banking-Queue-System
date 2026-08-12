import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { AppButton } from '../../../shared/components/app-button/app-button';
import { AppCard } from '../../../shared/components/app-card/app-card';
import { AppConfirmDialog } from '../../../shared/components/app-confirm-dialog/app-confirm-dialog';
import { AppDataTableShell } from '../../../shared/components/app-data-table-shell/app-data-table-shell';
import { AppEmptyState } from '../../../shared/components/app-empty-state/app-empty-state';
import { AppLoadingState } from '../../../shared/components/app-loading-state/app-loading-state';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AdminMappingsService } from '../../../core/services/admin-mappings.service';

@Component({
  selector: 'app-admin-mappings',
  imports: [
    CommonModule,
    FormsModule,
    AppButton,
    AppCard,
    AppConfirmDialog,
    AppDataTableShell,
    AppEmptyState,
    AppLoadingState,
    DashboardLayout,
    AppPageHeader,
  ],
  templateUrl: './admin-mappings.html',
  styleUrl: './admin-mappings.scss',
})
export class AdminMappings implements OnInit {
  private mappingService = inject(AdminMappingsService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  branchId = Number(sessionStorage.getItem('selectedBranchId')) || null;
  queueMachines: any[] = [];
  services: any[] = [];
  mappings: any[] = [];
  successMessage = '';
  errorMessage = '';
  isMachinesLoading = true;
  isServicesLoading = true;
  isMappingsLoading = true;
  isSaving = false;
  isDeleting = false;
  pendingDeleteMapping: any | null = null;

  selectedQueueMachineId: number | null = null;
  selectedServiceIds: number[] = [];

  get pendingDeleteMessage(): string {
    const machine =
      this.pendingDeleteMapping?.queueMachine?.machineName ||
      this.pendingDeleteMapping?.queueMachine?.machineCode ||
      'máy bốc số này';
    const service = this.pendingDeleteMapping?.service?.serviceName || 'dịch vụ đã chọn';
    return `Gỡ liên kết giữa ${machine} và ${service}?`;
  }

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

    this.isMachinesLoading = true;
    this.mappingService.getQueueMachines().subscribe({
      next: (data) => {
        this.queueMachines = (data || []).filter(
          (machine) => machine.branch?.branchId === this.branchId,
        );

        if (!this.selectedQueueMachineId && this.queueMachines.length > 0) {
          this.selectedQueueMachineId = this.queueMachines[0].queueMachineId;
        }

        this.isMachinesLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không tải được máy bốc số.');
        this.isMachinesLoading = false;
        this.cdr.detectChanges();
      },
    });

    this.isServicesLoading = true;
    this.mappingService.getServices(this.branchId).subscribe({
      next: (data) => {
        this.services = data || [];
        this.isServicesLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không tải được dịch vụ.');
        this.isServicesLoading = false;
        this.cdr.detectChanges();
      },
    });

    this.loadMappings();
  }

  loadMappings(): void {
    this.isMappingsLoading = true;
    this.mappingService.getMappings().subscribe({
      next: (data) => {
        this.mappings = (data || []).filter(
          (mapping) => mapping.queueMachine?.branch?.branchId === this.branchId,
        );
        this.isMappingsLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không tải được danh sách liên kết.');
        this.isMappingsLoading = false;
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

    this.isSaving = true;
    forkJoin(
      this.selectedServiceIds.map((serviceId) =>
        this.mappingService.createMapping(Number(this.selectedQueueMachineId), serviceId),
      ),
    ).subscribe({
      next: () => {
        this.successMessage = `Đã tạo ${this.selectedServiceIds.length} liên kết.`;
        this.selectedServiceIds = [];
        this.isSaving = false;
        this.loadMappings();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không tạo được liên kết. Có thể một liên kết đã tồn tại.',
        );
        this.isSaving = false;
        this.cdr.detectChanges();
      },
    });
  }

  deleteMapping(mapping: any): void {
    this.pendingDeleteMapping = mapping;
  }

  cancelDeleteMapping(): void {
    if (!this.isDeleting) {
      this.pendingDeleteMapping = null;
    }
  }

  confirmDeleteMapping(): void {
    if (!this.pendingDeleteMapping || this.isDeleting) {
      return;
    }

    const queueMachineId =
      this.pendingDeleteMapping.queueMachine?.queueMachineId ||
      this.pendingDeleteMapping.id?.queueMachineId;
    const serviceId =
      this.pendingDeleteMapping.service?.serviceId || this.pendingDeleteMapping.id?.serviceId;

    if (!queueMachineId || !serviceId) {
      this.errorMessage = 'Không xác định được liên kết cần xóa.';
      this.pendingDeleteMapping = null;
      this.cdr.detectChanges();
      return;
    }

    this.isDeleting = true;
    this.mappingService.deleteMapping(queueMachineId, serviceId).subscribe({
      next: () => {
        this.successMessage = 'Đã gỡ liên kết.';
        this.pendingDeleteMapping = null;
        this.isDeleting = false;
        this.loadMappings();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Gỡ liên kết thất bại.');
        this.isDeleting = false;
        this.cdr.detectChanges();
      },
    });
  }

  private ensureBranch(): this is this & { branchId: number } {
    if (!this.branchId) {
      this.errorMessage =
        'Tài khoản quản trị này chưa được gán chi nhánh. Hãy dùng tài khoản do quản trị viên hệ thống cấp.';
      this.isMachinesLoading = false;
      this.isServicesLoading = false;
      this.isMappingsLoading = false;
      this.cdr.detectChanges();
      return false;
    }

    return true;
  }
}
