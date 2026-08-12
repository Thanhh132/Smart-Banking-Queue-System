import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AppCard } from '../../../shared/components/app-card/app-card';
import { AppEmptyState } from '../../../shared/components/app-empty-state/app-empty-state';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';
import { AppLoadingState } from '../../../shared/components/app-loading-state/app-loading-state';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { AppStatusBadge } from '../../../shared/components/app-status-badge/app-status-badge';
import { QueueMonitor, ServingCounter } from '../../../core/models/queue-monitor.model';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { QueueMonitorService } from '../../../core/services/queue-monitor.service';

@Component({
  selector: 'app-queue-monitor',
  imports: [
    CommonModule,
    AppCard,
    AppEmptyState,
    AppIcon,
    AppLoadingState,
    AppPageHeader,
    AppStatusBadge,
    DashboardLayout,
  ],
  templateUrl: './queue-monitor.html',
  styleUrl: './queue-monitor.scss',
})
export class QueueMonitorComponent implements OnInit, OnDestroy {
  private static readonly REFRESH_INTERVAL_MS = 3000;
  private monitorService = inject(QueueMonitorService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  monitor: QueueMonitor | null = null;
  errorMessage = '';

  private intervalId: any;

  get servingCounterCount(): number {
    return (
      this.monitor?.servingCounters?.filter((counter) => counter.status === 'SERVING').length || 0
    );
  }

  get activeCounterCount(): number {
    return (
      this.monitor?.servingCounters?.filter((counter) => counter.status !== 'INACTIVE').length || 0
    );
  }

  get idleCounterCount(): number {
    return (
      this.monitor?.servingCounters?.filter((counter) => counter.status === 'IDLE').length || 0
    );
  }

  get currentlyServingCounters(): ServingCounter[] {
    return this.monitor?.servingCounters?.filter((counter) => counter.status === 'SERVING') || [];
  }

  /** Tải ngay lần đầu và tạm ngừng refresh khi tab bị ẩn. */
  ngOnInit(): void {
    this.loadMonitor();

    this.intervalId = setInterval(() => {
      if (document.visibilityState === 'visible') {
        this.loadMonitor();
      }
    }, QueueMonitorComponent.REFRESH_INTERVAL_MS);
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  /** Luôn lấy branchId từ phiên chọn chi nhánh để không hiển thị nhầm hàng đợi. */
  loadMonitor(): void {
    const branchId = sessionStorage.getItem('selectedBranchId');

    if (!branchId) {
      this.errorMessage = 'Chưa chọn chi nhánh.';
      this.cdr.detectChanges();
      return;
    }

    this.monitorService.getMonitor(Number(branchId)).subscribe({
      next: (data) => {
        this.monitor = data;
        this.errorMessage = '';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không tải được dữ liệu màn hình hàng đợi.',
        );
        this.cdr.detectChanges();
      },
    });
  }
}
