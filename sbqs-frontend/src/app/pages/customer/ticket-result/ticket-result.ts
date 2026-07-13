import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { QueueMonitor } from '../../../core/models/queue-monitor.model';
import { HistoryItem, HistoryService } from '../../../core/services/history.service';
import { QueueMonitorService } from '../../../core/services/queue-monitor.service';
import { TicketService } from '../../../core/services/ticket.service';
import { CustomerLiveTrackingService } from '../../../core/services/customer-live-tracking.service';
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
  private static readonly MONITOR_INTERVAL_MS = 3000;
  private monitorService = inject(QueueMonitorService);
  private ticketService = inject(TicketService);
  private historyService = inject(HistoryService);
  private cdr = inject(ChangeDetectorRef);
  readonly liveTracking = inject(CustomerLiveTrackingService);

  ticket: any = null;
  monitor: QueueMonitor | null = null;
  histories: HistoryItem[] = [];
  errorMessage = '';
  isCancelling = false;
  private isMonitorLoading = false;
  private intervalId: any;
  private lastTerminalStatus = '';
  private trackingEffect = effect(() => {
    const tracking = this.liveTracking.tracking();
    if (!tracking) {
      return;
    }

    this.ticket = {
      ...(this.ticket || {}),
      ticketId: tracking.ticketId,
      ticketNumber: tracking.ticketNumber,
      status: tracking.status,
      counterName: tracking.counterName,
      servingStartedAt: tracking.servingStartedAt,
      branchName: tracking.branchName,
      serviceName: tracking.serviceName,
      queueMachineLocationNote: tracking.queueMachineLocationNote,
    };

    if (
      ['COMPLETED', 'CANCELLED'].includes(tracking.status)
      && this.lastTerminalStatus !== tracking.status
    ) {
      this.lastTerminalStatus = tracking.status;
      this.loadHistory();
    }
  });

  get tracking() {
    return this.liveTracking.tracking();
  }

  get effectiveStatus(): string {
    return this.tracking?.status || this.ticket?.status || '';
  }

  ngOnInit(): void {
    const data = sessionStorage.getItem('currentTicket');

    if (data) {
      this.ticket = JSON.parse(data);
    }

    this.loadCurrentTicket();
    this.loadHistory();
    this.loadMonitor();
    this.intervalId = setInterval(() => {
      if (document.visibilityState === 'visible') {
        this.loadMonitor();
      }
    }, TicketResult.MONITOR_INTERVAL_MS);
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

  get queueMachineLocationNote(): string {
    return this.ticket?.queueMachine?.locationNote || this.ticket?.queueMachineLocationNote || '';
  }

  get ticketStatusLabel(): string {
    const labels: Record<string, string> = {
      WAITING: 'Đang chờ',
      SERVING: 'Đang phục vụ',
      COMPLETED: 'Đã hoàn thành',
      CANCELLED: 'Đã hủy',
    };
    return labels[this.effectiveStatus] || 'Đang xử lý';
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
        sessionStorage.setItem('currentTicket', JSON.stringify(ticket));
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
    if (this.isMonitorLoading) {
      return;
    }

    const branchId = Number(
      this.ticket?.branch?.branchId ||
        this.ticket?.branchId ||
        sessionStorage.getItem('selectedBranchId')
    );

    if (!branchId) {
      return;
    }

    const queueMachineId = Number(
      this.tracking?.queueMachineId || this.ticket?.queueMachine?.queueMachineId
    );

    this.isMonitorLoading = true;
    this.monitorService.getMonitor(branchId, queueMachineId || null).subscribe({
      next: (data) => {
        this.monitor = data;
        this.errorMessage = '';
        this.isMonitorLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isMonitorLoading = false;
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
          sessionStorage.setItem('currentTicket', JSON.stringify(ticket));
        } else {
          sessionStorage.removeItem('currentTicket');
        }

        this.loadMonitor();
        this.cdr.detectChanges();
      },
      error: () => {
        sessionStorage.removeItem('currentTicket');
        this.ticket = null;
        this.cdr.detectChanges();
      },
    });
  }
}
