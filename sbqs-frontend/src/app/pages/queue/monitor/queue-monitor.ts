import {
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';

import { AppCard } from '../../../shared/components/app-card/app-card';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { QueueMonitor } from '../../../core/models/queue-monitor.model';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { QueueMonitorService } from '../../../core/services/queue-monitor.service';

@Component({
  selector: 'app-queue-monitor',
  imports: [CommonModule, AppCard, DashboardLayout, AppPageHeader],
  templateUrl: './queue-monitor.html',
  styleUrl: './queue-monitor.scss',
})
export class QueueMonitorComponent implements OnInit, OnDestroy {
  private monitorService = inject(QueueMonitorService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  monitor: QueueMonitor | null = null;
  errorMessage = '';

  private intervalId: any;

  get servingCounterCount(): number {
    return this.monitor?.servingCounters?.filter((counter) => counter.status === 'SERVING').length || 0;
  }

  get activeCounterCount(): number {
    return this.monitor?.servingCounters?.filter((counter) => counter.status !== 'INACTIVE').length || 0;
  }

  getCounterStatusLabel(status: string): string {
    if (status === 'SERVING') {
      return 'Đang phục vụ';
    }

    if (status === 'IDLE') {
      return 'Đang rảnh';
    }

    return 'Không hoạt động';
  }

  ngOnInit(): void {
    this.loadMonitor();

    this.intervalId = setInterval(() => {
      this.loadMonitor();
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  loadMonitor(): void {
    const branchId = localStorage.getItem('selectedBranchId');

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
          'Không tải được dữ liệu màn hình hàng đợi.'
        );
        this.cdr.detectChanges();
      },
    });
  }
}
