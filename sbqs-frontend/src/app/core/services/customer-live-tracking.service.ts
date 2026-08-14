import { Injectable, inject, signal } from '@angular/core';
import { Subscription, catchError, exhaustMap, filter, of, switchMap, timer } from 'rxjs';

import { TicketService, TicketTracking } from './ticket.service';

export interface LiveTicketNotice {
  key: string;
  level: 'info' | 'warning' | 'urgent' | 'success';
  title: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class CustomerLiveTrackingService {
  private static readonly POLLING_INTERVAL_MS = 2000;
  private static readonly SEEN_NOTICE_KEYS = 'sbqs:seen-live-notices';
  private ticketService = inject(TicketService);
  private pollingSubscription: Subscription | null = null;
  private consumers = 0;
  private previousTicketId: number | null = null;
  private previousStatus = '';
  private previousPeopleAhead: number | null = null;
  private suppressedTicketId: number | null = null;

  readonly tracking = signal<TicketTracking | null>(null);
  readonly notice = signal<LiveTicketNotice | null>(null);
  readonly lastUpdatedAt = signal<Date | null>(null);

  /**
   * Đăng ký một consumer theo dõi phiếu. Bộ đếm giúp nhiều component dùng chung
   * service mà không tạo nhiều vòng polling song song.
   */
  start(): void {
    this.consumers++;
    if (this.pollingSubscription) {
      return;
    }

    this.pollingSubscription = timer(0, CustomerLiveTrackingService.POLLING_INTERVAL_MS)
      .pipe(
        filter(() => document.visibilityState === 'visible'),
        exhaustMap(() => this.loadTracking()),
      )
      .subscribe((tracking) => {
        if (tracking) {
          this.applyTracking(tracking);
        } else {
          this.clearTrackingState();
        }
      });
  }

  /** Chỉ dừng polling và xóa state khi consumer cuối cùng đã rời màn hình. */
  stop(): void {
    this.consumers = Math.max(0, this.consumers - 1);
    if (this.consumers > 0) {
      return;
    }
    this.pollingSubscription?.unsubscribe();
    this.pollingSubscription = null;
    this.resetState();
  }

  dismissNotice(): void {
    this.notice.set(null);
  }

  /**
   * Xóa ngay ticket vừa hủy và bỏ qua snapshot WAITING có thể đang bay về từ
   * một request polling cũ. Ticket mới có ID khác vẫn được theo dõi bình thường.
   */
  clearActiveTicket(ticketId: number): void {
    this.suppressedTicketId = ticketId;
    sessionStorage.removeItem('currentTicket');
    this.clearTrackingState();
  }

  private resetState(): void {
    this.suppressedTicketId = null;
    this.clearTrackingState();
  }

  private clearTrackingState(): void {
    this.previousTicketId = null;
    this.previousStatus = '';
    this.previousPeopleAhead = null;
    this.tracking.set(null);
    this.notice.set(null);
    this.lastUpdatedAt.set(null);
  }

  /** Ưu tiên ticket cache để giảm một API call; cache hỏng sẽ tự phục hồi từ server. */
  private loadTracking() {
    const cachedTicket = this.readCachedTicket();
    if (cachedTicket?.ticketId) {
      return this.ticketService.getTracking(cachedTicket.ticketId).pipe(
        catchError(() => {
          sessionStorage.removeItem('currentTicket');
          return this.loadCurrentTicketTracking();
        }),
      );
    }

    return this.loadCurrentTicketTracking();
  }

  private loadCurrentTicketTracking() {
    return this.ticketService.getCurrentTicket().pipe(
      switchMap((ticket) => {
        if (!ticket?.ticketId) {
          return of(null);
        }
        sessionStorage.setItem('currentTicket', JSON.stringify(ticket));
        return this.ticketService.getTracking(ticket.ticketId);
      }),
      catchError(() => of(null)),
    );
  }

  /**
   * So sánh snapshot mới với trạng thái trước đó để chỉ phát thông báo tại thời điểm
   * chuyển trạng thái hoặc khi số người phía trước vừa giảm xuống ngưỡng cảnh báo.
   */
  private applyTracking(tracking: TicketTracking): void {
    if (tracking.ticketId === this.suppressedTicketId) {
      return;
    }
    this.suppressedTicketId = null;

    if (this.previousTicketId !== tracking.ticketId) {
      this.previousTicketId = tracking.ticketId;
      this.previousStatus = '';
      this.previousPeopleAhead = null;
      this.notice.set(null);
    }

    const previousStatus = this.previousStatus;
    const previousPeopleAhead = this.previousPeopleAhead;
    this.tracking.set(tracking);
    this.lastUpdatedAt.set(new Date());
    if (['COMPLETED', 'CANCELLED', 'MISSED'].includes(tracking.status)) {
      sessionStorage.removeItem('currentTicket');
    } else {
      this.updateCachedTicket(tracking);
    }

    if (tracking.status === 'SERVING' && previousStatus !== 'SERVING') {
      this.showNotice({
        key: `serving:${tracking.ticketId}`,
        level: 'urgent',
        title: 'Đã đến lượt bạn',
        message: `Phiếu #${tracking.ticketNumber} đang được gọi tại ${tracking.counterName || 'quầy phục vụ'}.`,
      });
    } else if (
      tracking.status === 'WAITING' &&
      tracking.peopleAhead <= 3 &&
      (previousPeopleAhead === null || previousPeopleAhead > 3)
    ) {
      this.showNotice({
        key: `near:${tracking.ticketId}:${tracking.peopleAhead}`,
        level: 'warning',
        title: 'Sắp đến lượt bạn',
        message:
          tracking.peopleAhead === 0
            ? `Phiếu #${tracking.ticketNumber} đang ở lượt tiếp theo.`
            : `Còn ${tracking.peopleAhead} phiếu đang chờ trước bạn.`,
      });
    } else if (tracking.status === 'COMPLETED' && previousStatus !== 'COMPLETED') {
      this.showNotice({
        key: `completed:${tracking.ticketId}`,
        level: 'success',
        title: 'Giao dịch đã hoàn thành',
        message: `Phiếu #${tracking.ticketNumber} đã được cập nhật vào lịch sử.`,
      });
    } else if (tracking.status === 'CANCELLED' && previousStatus !== 'CANCELLED') {
      this.showNotice({
        key: `cancelled:${tracking.ticketId}`,
        level: 'info',
        title: 'Phiếu đã hủy',
        message: `Phiếu #${tracking.ticketNumber} không còn trong hàng đợi.`,
      });
    } else if (tracking.status === 'MISSED' && previousStatus !== 'MISSED') {
      this.showNotice({
        key: `missed:${tracking.ticketId}`,
        level: 'warning',
        title: 'Bạn đã lỡ lượt',
        message:
          'Nhân viên đã ghi nhận bạn không đến quầy. Vui lòng lấy số mới nếu vẫn cần giao dịch.',
      });
    }

    this.previousStatus = tracking.status;
    this.previousPeopleAhead = tracking.peopleAhead;
  }

  /** Ghi nhớ các thông báo đã hiện trong tab để route/polling không phát lại trạng thái cũ. */
  private showNotice(notice: LiveTicketNotice): void {
    const seenKeys = this.readSeenNoticeKeys();
    if (seenKeys.has(notice.key)) {
      return;
    }
    seenKeys.add(notice.key);
    sessionStorage.setItem(
      CustomerLiveTrackingService.SEEN_NOTICE_KEYS,
      JSON.stringify([...seenKeys].slice(-30)),
    );
    this.notice.set(notice);
  }

  private readSeenNoticeKeys(): Set<string> {
    try {
      const values = JSON.parse(
        sessionStorage.getItem(CustomerLiveTrackingService.SEEN_NOTICE_KEYS) || '[]',
      );
      const seenKeys = new Set<string>(
        Array.isArray(values)
          ? values.filter((value): value is string => typeof value === 'string')
          : [],
      );
      const legacyKey = sessionStorage.getItem('sbqs:last-live-notice');
      if (legacyKey) seenKeys.add(legacyKey);
      return seenKeys;
    } catch {
      sessionStorage.removeItem(CustomerLiveTrackingService.SEEN_NOTICE_KEYS);
      return new Set();
    }
  }

  private readCachedTicket(): any | null {
    const raw = sessionStorage.getItem('currentTicket');
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw);
    } catch {
      sessionStorage.removeItem('currentTicket');
      return null;
    }
  }

  private updateCachedTicket(tracking: TicketTracking): void {
    const cachedTicket = this.readCachedTicket() || {};
    sessionStorage.setItem(
      'currentTicket',
      JSON.stringify({
        ...cachedTicket,
        ticketId: tracking.ticketId,
        ticketNumber: tracking.ticketNumber,
        status: tracking.status,
        counterName: tracking.counterName,
        queueMachineLocationNote: tracking.queueMachineLocationNote,
        servingStartedAt: tracking.servingStartedAt,
      }),
    );
  }
}
