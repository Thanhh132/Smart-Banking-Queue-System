import {
  ChangeDetectorRef,
  Component,
  inject,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { AppCard } from '../../../shared/components/app-card/app-card';
import { AppButton } from '../../../shared/components/app-button/app-button';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';

import { AdminMappingsService } from '../../../core/services/admin-mappings.service';

@Component({
  selector: 'app-admin-mappings',
  imports: [
    CommonModule,
    FormsModule,
    AppCard,
    AppButton,
    DashboardLayout,
    AppPageHeader
  ],
  templateUrl: './admin-mappings.html',
  styleUrl: './admin-mappings.scss',
})
export class AdminMappings implements OnInit {

  private mappingService = inject(AdminMappingsService);
  private cdr = inject(ChangeDetectorRef);

  queueMachines: any[] = [];
  services: any[] = [];
  mappings: any[] = [];
  successMessage = '';
  errorMessage = '';

  selectedQueueMachineId: number | null = null;
  selectedServiceId: number | null = null;

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.mappingService.getQueueMachines().subscribe({
      next: (data) => {
        this.queueMachines = data;
        this.cdr.detectChanges();
      }
    });

    this.mappingService.getServices().subscribe({
      next: (data) => {
        this.services = data;
        this.cdr.detectChanges();
      }
    });

    this.loadMappings();
  }

  loadMappings(): void {
    this.mappingService.getMappings().subscribe({
      next: (data) => {
        this.mappings = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
      }
    });
  }

  createMapping(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (!this.selectedQueueMachineId || !this.selectedServiceId) {
      this.errorMessage = 'Vui lòng chọn máy bốc số và dịch vụ.';
      this.cdr.detectChanges();
      return;
    }

    this.mappingService
      .createMapping(
        this.selectedQueueMachineId,
        this.selectedServiceId
      )
      .subscribe({
        next: () => {
          this.successMessage = 'Tạo mapping thành công.';

          this.selectedQueueMachineId = null;
          this.selectedServiceId = null;

          this.loadMappings();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Create mapping error:', err);

          this.errorMessage =
            err?.error?.message ||
            err?.error ||
            'Tạo mapping thất bại.';

          this.cdr.detectChanges();
        }
      });
  }

  deleteMapping(mapping: any): void {
    const confirmed = confirm(
      'Bạn có chắc muốn gỡ mapping này không?'
    );

    if (!confirmed) {
      return;
    }

    const queueMachineId =
      mapping.queueMachine?.queueMachineId ||
      mapping.id?.queueMachineId;

    const serviceId =
      mapping.service?.serviceId ||
      mapping.id?.serviceId;

    if (!queueMachineId || !serviceId) {
      this.errorMessage = 'Không xác định được mapping cần xóa.';
      this.cdr.detectChanges();
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.mappingService
      .deleteMapping(queueMachineId, serviceId)
      .subscribe({
        next: () => {
          this.successMessage = 'Gỡ mapping thành công.';
          this.loadMappings();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Delete mapping error:', err);

          this.errorMessage =
            typeof err?.error === 'string'
              ? err.error
              : err?.error?.message || 'Gỡ mapping thất bại.';

          this.cdr.detectChanges();
        }
      });
  }
}