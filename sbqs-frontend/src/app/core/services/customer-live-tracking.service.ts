import { Injectable, inject, signal } from '@angular/core';
import { Subscription, catchError, exhaustMap, of, switchMap, timer } from 'rxjs';

import { TicketService, TicketTracking } from './ticket.service';

export interface LiveTicketNotice {
  key: string;
  level: 'info' | 'warning' | 'urgent' | 'success';
  title: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class CustomerLiveTrackingService {
  private ticketService = inject(TicketService);
  private pollingSubscription: Subscription | null = null;
  private consumers = 0;
  private previousTicketId: number | null = null;
  private previousStatus = '';
  private previousPeopleAhead: number | null = null;

  readonly tracking = signal<TicketTracking | null>(null);
  readonly notice = signal<LiveTicketNotice | null>(null);
  readonly lastUpdatedAt = signal<Date | null>(null);

  start(): void {
    this.consumers++;
    if (this.pollingSubscription) {
      return;
    }

    this.pollingSubscription = timer(0, 1000)
      .pipe(exhaustMap(() => this.loadTracking()))
      .subscribe((tracking) => {
        if (tracking) {
          this.applyTracking(tracking);
        }
      });
  }

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

  private resetState(): void {
    this.previousTicketId = null;
    this.previousStatus = '';
    this.previousPeopleAhead = null;
    this.tracking.set(null);
    this.notice.set(null);
    this.lastUpdatedAt.set(null);
    sessionStorage.removeItem('sbqs:last-live-notice');
  }

  private loadTracking() {
    const cachedTicket = this.readCachedTicket();
    if (cachedTicket?.ticketId) {
      return this.ticketService.getTracking(cachedTicket.ticketId).pipe(
        catchError(() => {
          localStorage.removeItem('currentTicket');
          return this.loadCurrentTicketTracking();
        })
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
        localStorage.setItem('currentTicket', JSON.stringify(ticket));
        return this.ticketService.getTracking(ticket.ticketId);
      }),
      catchError(() => of(null))
    );
  }

  private applyTracking(tracking: TicketTracking): void {
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
    this.updateCachedTicket(tracking);

    if (tracking.status === 'SERVING' && previousStatus !== 'SERVING') {
      this.showNotice({
        key: `serving:${tracking.ticketId}`,
        level: 'urgent',
        title: 'Đã đến lượt bạn',
        message: `Phiếu #${tracking.ticketNumber} đang được gọi tại ${tracking.counterName || 'quầy phục vụ'}.`,
      });
    } else if (
      tracking.status === 'WAITING'
      && tracking.peopleAhead <= 3
      && (previousPeopleAhead === null || previousPeopleAhead > 3)
    ) {
      this.showNotice({
        key: `near:${tracking.ticketId}:${tracking.peopleAhead}`,
        level: 'warning',
        title: 'Sắp đến lượt bạn',
        message: tracking.peopleAhead === 0
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
    }

    this.previousStatus = tracking.status;
    this.previousPeopleAhead = tracking.peopleAhead;
  }

  private showNotice(notice: LiveTicketNotice): void {
    if (sessionStorage.getItem('sbqs:last-live-notice') === notice.key) {
      return;
    }
    sessionStorage.setItem('sbqs:last-live-notice', notice.key);
    this.notice.set(notice);
  }

  private readCachedTicket(): any | null {
    const raw = localStorage.getItem('currentTicket');
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw);
    } catch {
      localStorage.removeItem('currentTicket');
      return null;
    }
  }

  private updateCachedTicket(tracking: TicketTracking): void {
    const cachedTicket = this.readCachedTicket() || {};
    localStorage.setItem('currentTicket', JSON.stringify({
      ...cachedTicket,
      ticketId: tracking.ticketId,
      ticketNumber: tracking.ticketNumber,
      status: tracking.status,
      counterName: tracking.counterName,
      queueMachineLocationNote: tracking.queueMachineLocationNote,
      servingStartedAt: tracking.servingStartedAt,
    }));
  }
}
