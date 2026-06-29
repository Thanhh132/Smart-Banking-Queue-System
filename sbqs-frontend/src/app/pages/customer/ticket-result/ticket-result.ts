import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { QueueMonitor } from '../../../core/models/queue-monitor.model';
import { HistoryItem, HistoryService } from '../../../core/services/history.service';
import { QueueMonitorService } from '../../../core/services/queue-monitor.service';
import { TicketService } from '../../../core/services/ticket.service';
import { ReportExportButtons } from '../../../shared/components/report-export-buttons/report-export-buttons';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({
  selector: 'app-ticket-result',
  standalone: true,
  imports: [
    CommonModule,
    DashboardLayout,
    RouterLink,
    ReportExportButtons,
    AppIcon,
  ],
  templateUrl: './ticket-result.html',
  styleUrl: './ticket-result.scss',
})
export class TicketResult implements OnInit, OnDestroy {
  private monitorService = inject(QueueMonitorService);
  private ticketService = inject(TicketService);
  private historyService = inject(HistoryService);
  private cdr = inject(ChangeDetectorRef);

  ticket: any = null;
  monitor: QueueMonitor | null = null;
  histories: HistoryItem[] = [];
  errorMessage = '';
  isCancelling = false;
  private intervalId: any;

  ngOnInit(): void {
    const data = localStorage.getItem('currentTicket');

    if (data) {
      this.ticket = JSON.parse(data);
    }

    this.loadCurrentTicket();
    this.loadHistory();
    this.loadMonitor();
    this.intervalId = setInterval(() => this.loadMonitor(), 2000);
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  get serviceName(): string {
    return this.ticket?.service?.serviceName || this.ticket?.serviceName || 'Chưa xác định';
  }

  get branchName(): string {
    return this.ticket?.branch?.branchName || this.ticket?.branchName || this.monitor?.branchName || 'Chưa xác định';
  }

  get queueMachineName(): string {
    return this.ticket?.queueMachine?.machineName || 'Máy bốc số';
  }

  get ticketStatusLabel(): string {
    const labels: Record<string, string> = {
      WAITING: 'Đang chờ',
      SERVING: 'Đang phục vụ',
      COMPLETED: 'Đã hoàn thành',
      CANCELLED: 'Đã hủy',
    };
    return labels[this.ticket?.status] || 'Đang xử lý';
  }

  get servingCounterCount(): number {
    return this.monitor?.servingCounters?.filter((counter) => counter.status === 'SERVING').length || 0;
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

  getCounterStatusClass(status: string): string {
    return `customer-counter--${status.toLowerCase()}`;
  }

  statusLabel(status?: string): string {
    const labels: Record<string, string> = {
      COMPLETED: 'Hoàn thành',
      CANCELLED: 'Đã hủy',
    };
    return labels[status || ''] || status || '-';
  }

  formatDate(value?: string): string {
    return value ? new Date(value).toLocaleString('vi-VN') : '-';
  }

  cancelTicket(): void {
    if (!this.ticket?.ticketId) {
      return;
    }

    this.isCancelling = true;
    this.errorMessage = '';
    this.ticketService.cancelTicket(this.ticket.ticketId).subscribe({
      next: (ticket: any) => {
        this.ticket = ticket;
        localStorage.setItem('currentTicket', JSON.stringify(ticket));
        this.isCancelling = false;
        this.loadHistory();
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Không hủy được phiếu này.';
        this.isCancelling = false;
        this.cdr.detectChanges();
      },
    });
  }

  private loadHistory(): void {
    this.historyService.getHistory().subscribe({
      next: (histories) => {
        this.histories = histories || [];
        this.cdr.detectChanges();
      },
      error: () => {
        this.histories = [];
        this.cdr.detectChanges();
      },
    });
  }

  private loadMonitor(): void {
    const branchId = Number(
      this.ticket?.branch?.branchId ||
        this.ticket?.branchId ||
        localStorage.getItem('selectedBranchId')
    );

    if (!branchId) {
      return;
    }

    const queueMachineId = Number(this.ticket?.queueMachine?.queueMachineId);

    this.monitorService.getMonitor(branchId, queueMachineId || null).subscribe({
      next: (data) => {
        this.monitor = data;
        this.errorMessage = '';
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Không tải được bảng gọi số.';
        this.cdr.detectChanges();
      },
    });
  }

  private loadCurrentTicket(): void {
    this.ticketService.getCurrentTicket().subscribe({
      next: (ticket: any) => {
        this.ticket = ticket || null;

        if (ticket) {
          localStorage.setItem('currentTicket', JSON.stringify(ticket));
        } else {
          localStorage.removeItem('currentTicket');
        }

        this.loadMonitor();
        this.cdr.detectChanges();
      },
      error: () => {
        localStorage.removeItem('currentTicket');
        this.ticket = null;
        this.cdr.detectChanges();
      },
    });
  }
}
