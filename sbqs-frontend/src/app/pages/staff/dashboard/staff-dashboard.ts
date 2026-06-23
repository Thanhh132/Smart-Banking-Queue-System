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
  pendingApprovalTasks: any[] = [];
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loadCounters();
    this.loadAssignedCounter();
    this.loadPendingApprovalTasks();
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
            'Chi nhánh này chưa có quầy giao dịch. Branch Admin cần tạo quầy trước.';
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không tải được trạng thái quầy.'
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
          'Không tải được ca làm hiện tại.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  loadPendingApprovalTasks(): void {
    this.staffService.getPendingApprovalTasks().subscribe({
      next: (tasks: any[]) => {
        this.pendingApprovalTasks = tasks || [];
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không tải được danh sách phiếu chờ duyệt.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  assignCounter(): void {
    if (!this.selectedCounterId) {
      this.errorMessage = 'Hãy chọn quầy để bắt đầu ca.';
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.staffService.assignCounter(this.selectedCounterId).subscribe({
      next: (counter: any) => {
        this.selectedCounter = counter;
        this.currentTicket = counter.currentTicket || null;
        this.successMessage = `Đã assign vào ${counter.counterName}.`;
        this.loadDashboard();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Assign quầy thất bại.');
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
        this.successMessage = 'Đã kết thúc ca và đóng quầy.';
        this.selectedCounter = null;
        this.currentTicket = null;
        this.loadDashboard();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Unassign quầy thất bại.');
        this.cdr.detectChanges();
      },
    });
  }

  callNext(): void {
    this.errorMessage = '';

    if (!this.selectedCounter?.counterId) {
      this.errorMessage = 'Hãy assign vào một quầy trước khi gọi số.';
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
          'Không gọi được khách tiếp theo.'
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
          'Không hoàn thành được ticket.'
        );
        this.cdr.detectChanges();
      },
    });
  }
}
