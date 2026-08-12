import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { HistoryItem, HistoryService } from '../../../core/services/history.service';
import { AppDataTableShell } from '../../../shared/components/app-data-table-shell/app-data-table-shell';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { AppStatusBadge } from '../../../shared/components/app-status-badge/app-status-badge';
import { ReportExportButtons } from '../../../shared/components/report-export-buttons/report-export-buttons';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({
  selector: 'app-staff-history',
  standalone: true,
  imports: [
    CommonModule,
    DashboardLayout,
    AppDataTableShell,
    AppPageHeader,
    AppStatusBadge,
    ReportExportButtons,
  ],
  templateUrl: './staff-history.html',
  styleUrl: './staff-history.scss',
})
export class StaffHistory implements OnInit {
  private historyService = inject(HistoryService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  histories: HistoryItem[] = [];
  isLoading = true;
  errorMessage = '';

  get completedCount(): number {
    return this.histories.filter((item) => item.status === 'COMPLETED').length;
  }

  get missedCount(): number {
    return this.histories.filter((item) => item.status === 'MISSED').length;
  }

  ngOnInit(): void {
    this.historyService.getHistory().subscribe({
      next: (histories) => {
        this.histories = histories || [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.errorMessage = this.apiError.getMessage(error, 'Không tải được lịch sử phục vụ.');
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  statusLabel(status?: string): string {
    const labels: Record<string, string> = {
      COMPLETED: 'Hoàn thành',
      CANCELLED: 'Đã hủy',
      MISSED: 'Khách không đến',
    };
    return labels[status || ''] || status || '-';
  }

  formatDate(value?: string): string {
    return value ? new Date(value).toLocaleString('vi-VN') : '-';
  }
}
