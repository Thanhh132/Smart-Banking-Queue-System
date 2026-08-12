import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { HistoryItem, HistoryService } from '../../../core/services/history.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppDataTableShell } from '../../../shared/components/app-data-table-shell/app-data-table-shell';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { AppStatusBadge } from '../../../shared/components/app-status-badge/app-status-badge';

@Component({
  selector: 'app-admin-history',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DashboardLayout,
    AppDataTableShell,
    AppPageHeader,
    AppStatusBadge,
  ],
  templateUrl: './admin-history.html',
  styleUrl: './admin-history.scss',
})
export class AdminHistory implements OnInit {
  private historyService = inject(HistoryService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  histories: HistoryItem[] = [];
  staffOptions: { id: number; name: string }[] = [];
  selectedStaffId = 0;
  status = 'ALL';
  isLoading = true;
  errorMessage = '';

  private buildStaffOptions(items: HistoryItem[]): { id: number; name: string }[] {
    const values = new Map<number, string>();
    items.forEach((item) => {
      if (item.staffId && item.staffName) values.set(item.staffId, item.staffName);
    });
    return [...values]
      .map(([id, name]) => ({ id, name }))
      .sort((a, b) => a.name.localeCompare(b.name, 'vi'));
  }

  get filteredHistories(): HistoryItem[] {
    return this.histories.filter(
      (item) =>
        (!this.selectedStaffId || item.staffId === this.selectedStaffId) &&
        (this.status === 'ALL' || item.status === this.status),
    );
  }

  get completedCount(): number {
    return this.filteredHistories.filter((item) => item.status === 'COMPLETED').length;
  }

  get missedCount(): number {
    return this.filteredHistories.filter((item) => item.status === 'MISSED').length;
  }

  get hasActiveFilters(): boolean {
    return this.selectedStaffId !== 0 || this.status !== 'ALL';
  }

  ngOnInit(): void {
    this.historyService.getHistory().subscribe({
      next: (items) => {
        this.histories = items || [];
        this.staffOptions = this.buildStaffOptions(this.histories);
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

  statusLabel(value?: string): string {
    return (
      (
        { COMPLETED: 'Hoàn thành', CANCELLED: 'Khách hủy', MISSED: 'Khách không đến' } as Record<
          string,
          string
        >
      )[value || ''] ||
      value ||
      '-'
    );
  }

  formatDate(value?: string): string {
    return value ? new Date(value).toLocaleString('vi-VN') : '-';
  }
}
