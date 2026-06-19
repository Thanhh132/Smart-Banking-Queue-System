import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

import { QueueMonitor } from '../../../core/models/queue-monitor.model';
import { QueueMonitorService } from '../../../core/services/queue-monitor.service';
import { TicketService } from '../../../core/services/ticket.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { AppCard } from '../../../shared/components/app-card/app-card';

@Component({
  selector: 'app-ticket-result',
  imports: [
    CommonModule,
    DashboardLayout,
    AppPageHeader,
    AppCard,
  ],
  templateUrl: './ticket-result.html',
  styleUrl: './ticket-result.scss',
})
export class TicketResult implements OnInit, OnDestroy {
  private monitorService = inject(QueueMonitorService);
  private ticketService = inject(TicketService);
  private cdr = inject(ChangeDetectorRef);

  ticket: any = null;
  monitor: QueueMonitor | null = null;
  errorMessage = '';
  private intervalId: any;

  ngOnInit(): void {
    const data = localStorage.getItem('currentTicket');

    if (data) {
      this.ticket = JSON.parse(data);
    }

    this.loadCurrentTicket();
    this.loadMonitor();
    this.intervalId = setInterval(() => this.loadMonitor(), 2000);
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  get serviceName(): string {
    return this.ticket?.service?.serviceName || this.ticket?.serviceName || 'Chua xac dinh';
  }

  get branchName(): string {
    return this.ticket?.branch?.branchName || this.ticket?.branchName || this.monitor?.branchName || 'Chua xac dinh';
  }

  get queueMachineName(): string {
    return this.ticket?.queueMachine?.machineName || 'May boc so';
  }

  get servingCounterCount(): number {
    return this.monitor?.servingCounters?.filter((counter) => counter.status === 'SERVING').length || 0;
  }

  getCounterStatusLabel(status: string): string {
    if (status === 'SERVING') {
      return 'Dang phuc vu';
    }

    if (status === 'IDLE') {
      return 'Dang ranh';
    }

    return 'Khong hoat dong';
  }

  cancelTicket(): void {
    if (!this.ticket?.ticketId) {
      return;
    }

    this.ticketService.cancelTicket(this.ticket.ticketId).subscribe({
      next: (ticket: any) => {
        this.ticket = ticket;
        localStorage.setItem('currentTicket', JSON.stringify(ticket));
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Khong huy duoc ticket nay.';
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
        this.errorMessage = 'Khong tai duoc bang goi so.';
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
