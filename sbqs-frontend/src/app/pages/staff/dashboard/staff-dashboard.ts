import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, finalize, forkJoin, of } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { HistoryItem, HistoryService } from '../../../core/services/history.service';
import { StaffService } from '../../../core/services/staff.service';
import { AppCard } from '../../../shared/components/app-card/app-card';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { ReportExportButtons } from '../../../shared/components/report-export-buttons/report-export-buttons';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({
  selector: 'app-staff-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DashboardLayout,
    AppPageHeader,
    AppCard,
    ReportExportButtons,
    AppIcon,
  ],
  templateUrl: './staff-dashboard.html',
  styleUrl: './staff-dashboard.scss',
})
export class StaffDashboard implements OnInit, OnDestroy {
  private static readonly LIVE_REFRESH_INTERVAL_MS = 2000;
  private staffService = inject(StaffService);
  private historyService = inject(HistoryService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  currentTicket: any = null;
  counters: any[] = [];
  selectedCounter: any = null;
  selectedCounterId: number | null = null;
  pendingApprovalTasks: any[] = [];
  histories: HistoryItem[] = [];
  errorMessage = '';
  successMessage = '';
  isCallingNext = false;
  isCompleting = false;
  isSkipping = false;
  isLiveRefreshing = false;
  lastUpdatedAt: Date | null = null;
  private liveIntervalId: any;
  delegationCode = '';
  delegationIdentity = '';
  verifiedDelegation: any = null;
  isVerifyingDelegation = false;

  /** Khởi tạo dashboard và chỉ polling khi tab đang hiển thị để giảm tải API. */
  ngOnInit(): void {
    this.loadDashboard();
    this.liveIntervalId = setInterval(() => {
      if (document.visibilityState === 'visible') {
        this.refreshLiveState();
      }
    }, StaffDashboard.LIVE_REFRESH_INTERVAL_MS);
  }

  ngOnDestroy(): void {
    if (this.liveIntervalId) {
      clearInterval(this.liveIntervalId);
    }
  }

  get availableCounters(): any[] {
    return this.counters.filter((counter) => counter.status !== 'ACTIVE');
  }

  get todayCompletedCount(): number {
    const today = new Date().toDateString();
    return this.histories.filter(
      (item) => item.status === 'COMPLETED' && item.completedAt && new Date(item.completedAt).toDateString() === today
    ).length;
  }

  loadDashboard(): void {
    this.loadCounters();
    this.loadAssignedCounter();
    this.loadPendingApprovalTasks();
    this.loadHistory();
  }

  loadCounters(): void {
    this.staffService.getCounters().subscribe({
      next: (counters: any[]) => {
        this.counters = counters || [];
        this.selectedCounterId =
          this.selectedCounter?.counterId ||
          this.availableCounters[0]?.counterId ||
          null;

        if (this.counters.length === 0) {
          this.errorMessage =
            'Chi nhánh này chưa có quầy giao dịch. Quản trị viên chi nhánh cần tạo quầy trước.';
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không tải được trạng thái quầy.');
        this.cdr.detectChanges();
      },
    });
  }

  loadAssignedCounter(): void {
    this.staffService.getAssignedCounter().subscribe({
      next: (counter: any) => {
        this.selectedCounter = counter || null;
        this.setServingTicket(counter?.currentTicket || null);

        if (counter?.counterId) {
          this.selectedCounterId = counter.counterId;
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không tải được ca làm hiện tại.');
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
        this.setServingTicket(counter.currentTicket || null);
        this.successMessage = `Đã vào ${counter.counterName}.`;
        this.loadDashboard();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Vào quầy thất bại.');
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
        this.errorMessage = this.apiError.getMessage(err, 'Kết thúc ca thất bại.');
        this.cdr.detectChanges();
      },
    });
  }

  /** Khóa nút trong lúc gọi API và không cho gọi số mới khi quầy còn phiếu hiện tại. */
  callNext(): void {
    if (this.isCallingNext || this.currentTicket) {
      return;
    }
    this.errorMessage = '';

    if (!this.selectedCounter?.counterId) {
      this.errorMessage = 'Hãy vào một quầy trước khi gọi số.';
      this.cdr.detectChanges();
      return;
    }

    this.isCallingNext = true;
    this.staffService.callNext(this.selectedCounter.counterId)
      .pipe(finalize(() => {
        this.isCallingNext = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
      next: (ticket: any) => {
        this.currentTicket = ticket;
        this.successMessage = `Đã gọi phiếu #${ticket.ticketNumber}.`;
        this.refreshLiveState();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không gọi được khách tiếp theo.');
        this.cdr.detectChanges();
      },
    });
  }

  /** Hoàn tất phiếu hiện tại; backend sẽ đồng thời đóng workflow, ghi lịch sử và giải phóng quầy. */
  complete(): void {
    if (!this.currentTicket || this.isCompleting) {
      return;
    }

    this.errorMessage = '';

    this.isCompleting = true;
    this.staffService.complete(this.currentTicket.ticketId)
      .pipe(finalize(() => {
        this.isCompleting = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
      next: () => {
        this.currentTicket = null;
        this.loadDashboard();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không hoàn thành được phiếu.');
        this.cdr.detectChanges();
      },
    });
  }

  markNoShow(): void {
    if (!this.currentTicket || this.isSkipping) return;
    if (!confirm(`Xác nhận phiếu #${this.currentTicket.ticketNumber} không đến quầy?`)) return;

    this.errorMessage = '';
    this.isSkipping = true;
    this.staffService.markNoShow(this.currentTicket.ticketId)
      .pipe(finalize(() => {
        this.isSkipping = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: () => {
          this.successMessage = 'Đã ghi nhận khách không đến. Quầy có thể gọi số tiếp theo.';
          this.currentTicket = null;
          this.loadDashboard();
        },
        error: (err) => {
          this.errorMessage = this.apiError.getMessage(err, 'Không thể bỏ qua phiếu này.');
        },
      });
  }

  /** Gộp trạng thái quầy và task chờ trong một nhịp refresh, đồng thời chống request chồng nhau. */
  private refreshLiveState(): void {
    if (this.isLiveRefreshing || this.isCallingNext || this.isCompleting || this.isSkipping) {
      return;
    }

    this.isLiveRefreshing = true;
    forkJoin({
      counter: this.staffService.getAssignedCounter().pipe(catchError(() => of(null))),
      tasks: this.staffService.getPendingApprovalTasks().pipe(catchError(() => of([]))),
    }).pipe(finalize(() => {
      this.isLiveRefreshing = false;
      this.cdr.detectChanges();
    })).subscribe(({ counter, tasks }) => {
      this.selectedCounter = counter || null;
      this.setServingTicket(counter?.currentTicket || null);
      this.pendingApprovalTasks = tasks || [];
      this.lastUpdatedAt = new Date();
    });
  }

  /**
   * Giữ phần hồ sơ paperless đã tải khi polling chỉ trả ticket rút gọn; chỉ gọi API
   * chi tiết khi quầy bắt đầu phục vụ một ticket khác.
   */
  private setServingTicket(ticket: any): void {
    if (!ticket) {
      this.currentTicket = null;
      return;
    }

    if (
      this.currentTicket?.ticketId === ticket.ticketId &&
      Array.isArray(this.currentTicket?.paperlessFields)
    ) {
      this.currentTicket = {
        ...this.currentTicket,
        status: ticket.status,
        servingStartedAt: ticket.servingStartedAt || this.currentTicket.servingStartedAt,
      };
      return;
    }

    this.currentTicket = ticket;

    if (ticket.status !== 'SERVING') {
      return;
    }

    this.staffService.getTicketStaffView(ticket.ticketId).subscribe({
      next: (detail: any) => {
        if (this.currentTicket?.ticketId === detail?.ticketId) {
          this.currentTicket = detail;
          this.cdr.detectChanges();
        }
      },
      error: () => {
        // Khong chan man hinh goi so neu chi tiet ho so tam thoi chua tai duoc.
      },
    });
  }

  loadHistory(): void {
    this.historyService.getHistory().subscribe({
      next: (histories) => {
        this.histories = histories || [];
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không tải được lịch sử phục vụ.');
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

  verifyDelegation(): void {
    if (!this.delegationCode.trim() || !this.delegationIdentity.trim() || this.isVerifyingDelegation) return;
    this.isVerifyingDelegation = true; this.errorMessage = '';
    this.staffService.verifyDelegation(this.delegationCode, this.delegationIdentity).pipe(finalize(() => { this.isVerifyingDelegation = false; this.cdr.detectChanges(); })).subscribe({
      next: (result) => { this.verifiedDelegation = result; this.successMessage = 'Đã xác minh đúng mã ủy quyền và CCCD.'; this.delegationIdentity = ''; this.cdr.detectChanges(); },
      error: (err) => { this.verifiedDelegation = null; this.errorMessage = this.apiError.getMessage(err, 'Không xác minh được ủy quyền.'); this.cdr.detectChanges(); },
    });
  }

  acceptDelegation(): void {
    if (!this.verifiedDelegation) return;
    this.staffService.markDelegationUsed(this.verifiedDelegation.delegationId).subscribe({
      next: () => { this.successMessage = 'Đã tiếp nhận và đóng lệnh ủy quyền.'; this.verifiedDelegation = null; this.delegationCode = ''; this.cdr.detectChanges(); },
      error: (err) => { this.errorMessage = this.apiError.getMessage(err, 'Không thể đóng lệnh ủy quyền.'); this.cdr.detectChanges(); },
    });
  }
}
