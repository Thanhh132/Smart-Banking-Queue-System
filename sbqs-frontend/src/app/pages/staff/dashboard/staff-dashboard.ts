import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { AppButton } from '../../../shared/components/app-button/app-button';
import { AppCard } from '../../../shared/components/app-card/app-card';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { StaffService } from '../../../core/services/staff.service';

@Component({
  selector: 'app-staff-dashboard',
  imports: [
    CommonModule,
    FormsModule,
    DashboardLayout,
    AppPageHeader,
    AppButton,
    AppCard,
  ],
  templateUrl: './staff-dashboard.html',
  styleUrl: './staff-dashboard.scss',
})
export class StaffDashboard implements OnInit {
  private staffService = inject(StaffService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  currentTicket: any = null;
  counters: any[] = [];
  selectedCounter: any = null;
  selectedCounterId: number | null = null;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loadCounters();
    this.loadAssignedCounter();
  }

  loadCounters(): void {
    this.staffService.getCounters().subscribe({
      next: (counters: any[]) => {
        this.counters = counters || [];
        this.selectedCounterId =
          this.selectedCounter?.counterId ||
          this.counters.find((item) => item.status !== 'ACTIVE')?.counterId ||
          null;

        if (this.counters.length === 0) {
          this.errorMessage =
            'Chi nhanh nay chua co quay giao dich. Branch Admin can tao quay truoc.';
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Khong tai duoc trang thai quay.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  loadAssignedCounter(): void {
    this.staffService.getAssignedCounter().subscribe({
      next: (counter: any) => {
        this.selectedCounter = counter || null;
        this.currentTicket = counter?.currentTicket || null;

        if (counter?.counterId) {
          this.selectedCounterId = counter.counterId;
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Khong tai duoc ca lam hien tai.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  assignCounter(): void {
    if (!this.selectedCounterId) {
      this.errorMessage = 'Hay chon quay de bat dau ca.';
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.staffService.assignCounter(this.selectedCounterId).subscribe({
      next: (counter: any) => {
        this.selectedCounter = counter;
        this.currentTicket = counter.currentTicket || null;
        this.successMessage = `Da assign vao ${counter.counterName}.`;
        this.loadDashboard();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Assign quay that bai.');
        this.cdr.detectChanges();
      },
    });
  }

  unassignCounter(): void {
    if (!this.selectedCounter?.counterId) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.staffService.unassignCounter(this.selectedCounter.counterId).subscribe({
      next: () => {
        this.successMessage = 'Da ket thuc ca va dong quay.';
        this.selectedCounter = null;
        this.currentTicket = null;
        this.loadDashboard();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Unassign quay that bai.');
        this.cdr.detectChanges();
      },
    });
  }

  callNext(): void {
    this.errorMessage = '';

    if (!this.selectedCounter?.counterId) {
      this.errorMessage = 'Hay assign vao mot quay truoc khi goi so.';
      this.cdr.detectChanges();
      return;
    }

    this.staffService.callNext(this.selectedCounter.counterId).subscribe({
      next: (ticket: any) => {
        this.currentTicket = ticket;
        this.loadDashboard();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Khong goi duoc khach tiep theo.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  complete(): void {
    if (!this.currentTicket) {
      return;
    }

    this.errorMessage = '';

    this.staffService.complete(this.currentTicket.ticketId).subscribe({
      next: () => {
        this.currentTicket = null;
        this.loadDashboard();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Khong hoan thanh duoc ticket.'
        );
        this.cdr.detectChanges();
      },
    });
  }
}
