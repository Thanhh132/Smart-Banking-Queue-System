import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { HistoryItem, HistoryService } from '../../../core/services/history.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { AppCard } from '../../../shared/components/app-card/app-card';

@Component({
  selector: 'app-admin-history',
  standalone: true,
  imports: [CommonModule, FormsModule, DashboardLayout, AppPageHeader, AppCard],
  templateUrl: './admin-history.html',
  styleUrl: './admin-history.scss',
})
export class AdminHistory implements OnInit {
  private historyService = inject(HistoryService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  histories: HistoryItem[] = [];
  selectedStaffId = 0;
  status = 'ALL';
  errorMessage = '';

  get staffOptions(): { id: number; name: string }[] {
    const values = new Map<number, string>();
    this.histories.forEach((item) => {
      if (item.staffId && item.staffName) values.set(item.staffId, item.staffName);
    });
    return [...values].map(([id, name]) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name, 'vi'));
  }

  get filteredHistories(): HistoryItem[] {
    return this.histories.filter((item) =>
      (!this.selectedStaffId || item.staffId === this.selectedStaffId)
      && (this.status === 'ALL' || item.status === this.status),
    );
  }

  get completedCount(): number {
    return this.filteredHistories.filter((item) => item.status === 'COMPLETED').length;
  }

  get missedCount(): number {
    return this.filteredHistories.filter((item) => item.status === 'MISSED').length;
  }

  ngOnInit(): void {
    this.historyService.getHistory().subscribe({
      next: (items) => {
        this.histories = items || [];
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.errorMessage = this.apiError.getMessage(error, 'Không tải được lịch sử phục vụ.');
        this.cdr.detectChanges();
      },
    });
  }

  statusLabel(value?: string): string {
    return ({ COMPLETED: 'Hoàn thành', CANCELLED: 'Khách hủy', MISSED: 'Khách không đến' } as Record<string, string>)[value || ''] || value || '-';
  }

  formatDate(value?: string): string {
    return value ? new Date(value).toLocaleString('vi-VN') : '-';
  }
}
