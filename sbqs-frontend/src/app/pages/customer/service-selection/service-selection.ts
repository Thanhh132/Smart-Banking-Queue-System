import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { Service } from '../../../core/models/service.model';
import { QueueMonitor } from '../../../core/models/queue-monitor.model';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { QueueMonitorService } from '../../../core/services/queue-monitor.service';
import { ServicesService } from '../../../core/services/services.service';
import { TicketService } from '../../../core/services/ticket.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppCard } from '../../../shared/components/app-card/app-card';

@Component({
  selector: 'app-service-selection',
  imports: [CommonModule, DashboardLayout, AppCard],
  templateUrl: './service-selection.html',
  styleUrl: './service-selection.scss',
})
export class ServiceSelection implements OnInit {
  private servicesService = inject(ServicesService);
  private monitorService = inject(QueueMonitorService);
  private ticketService = inject(TicketService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  services: Service[] = [];
  monitor: QueueMonitor | null = null;
  errorMessage = '';
  isLoading = false;
  isCreatingTicket = false;

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

  ngOnInit(): void {
    this.loadPage();
    this.syncCurrentTicket();
  }

  loadPage(): void {
    const branchId = this.getBranchId();

    if (!branchId) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.servicesService.getMappedServicesByBranch(branchId).subscribe({
      next: (data) => {
        this.services = data || [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Khong tai duoc danh sach dich vu.'
        );
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });

    this.monitorService.getMonitor(branchId).subscribe({
      next: (data) => {
        this.monitor = data;
        this.cdr.detectChanges();
      },
      error: () => {
        this.monitor = null;
        this.cdr.detectChanges();
      },
    });
  }

  selectService(service: Service): void {
    const branchId = this.getBranchId();

    if (!branchId) {
      return;
    }

    const currentTicket = this.getCurrentActiveTicket();
    if (currentTicket) {
      this.errorMessage =
        'Ban dang co mot ticket chua hoan thanh. Hay theo doi hoac huy ticket hien tai truoc.';
      this.router.navigate(['/ticket']);
      return;
    }

    this.isCreatingTicket = true;
    this.errorMessage = '';

    this.ticketService.createTicket(branchId, service.serviceId).subscribe({
      next: (ticket: any) => {
        localStorage.setItem('currentTicket', JSON.stringify(ticket));
        this.isCreatingTicket = false;
        this.router.navigate(['/ticket']);
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Khong tao duoc ticket. Vui long thu lai.'
        );
        this.isCreatingTicket = false;
        this.cdr.detectChanges();
      },
    });
  }

  private getBranchId(): number | null {
    const branchId = Number(localStorage.getItem('selectedBranchId'));

    if (!branchId) {
      this.errorMessage = 'Chua chon chi nhanh.';
      this.cdr.detectChanges();
      return null;
    }

    return branchId;
  }

  private getCurrentActiveTicket(): any | null {
    const rawTicket = localStorage.getItem('currentTicket');

    if (!rawTicket) {
      return null;
    }

    try {
      const ticket = JSON.parse(rawTicket);
      return ['WAITING', 'SERVING'].includes(ticket?.status) ? ticket : null;
    } catch {
      localStorage.removeItem('currentTicket');
      return null;
    }
  }

  private syncCurrentTicket(): void {
    this.ticketService.getCurrentTicket().subscribe({
      next: (ticket: any) => {
        if (ticket) {
          localStorage.setItem('currentTicket', JSON.stringify(ticket));
        } else {
          localStorage.removeItem('currentTicket');
        }
      },
      error: () => {
        localStorage.removeItem('currentTicket');
      },
    });
  }
}
