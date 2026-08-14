import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { HistoryService } from '../../../core/services/history.service';
import { CustomerLiveTrackingService } from '../../../core/services/customer-live-tracking.service';
import { QueueMonitorService } from '../../../core/services/queue-monitor.service';
import { TicketService } from '../../../core/services/ticket.service';
import { TicketResult } from './ticket-result';

describe('TicketResult', () => {
  let component: TicketResult;
  let fixture: ComponentFixture<TicketResult>;
  const ticketService = {
    getCurrentTicket: vi.fn(() => of(null)),
    cancelTicket: vi.fn(() => of({ ticketId: 9, ticketNumber: 108, status: 'CANCELLED' })),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [TicketResult],
      providers: [
        provideRouter([]),
        { provide: HistoryService, useValue: { getHistory: () => of([]) } },
        { provide: QueueMonitorService, useValue: { getMonitor: () => of(null) } },
        { provide: TicketService, useValue: ticketService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TicketResult);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('uses shared confirmation before cancelling a waiting ticket', async () => {
    component.ticket = { ticketId: 9, ticketNumber: 108, status: 'WAITING' };
    component.cancelTicket();
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();

    expect(ticketService.cancelTicket).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('app-confirm-dialog [role="dialog"]')).toBeTruthy();
    component.confirmCancelTicket();
    expect(ticketService.cancelTicket).toHaveBeenCalledWith(9);
  });

  it('shows the cancelled status immediately even when polling still has a waiting snapshot', () => {
    const liveTracking = TestBed.inject(CustomerLiveTrackingService);
    liveTracking.tracking.set({
      ticketId: 9,
      ticketNumber: 108,
      status: 'WAITING',
      peopleAhead: 0,
      counterName: null,
      branchName: 'BIDV Phú Cường 9',
      serviceName: 'Làm thẻ vật lý',
      queueMachineId: 1,
      queueMachineLocationNote: 'Tầng 1',
      servingStartedAt: null,
    });
    component.ticket = { ticketId: 9, ticketNumber: 108, status: 'WAITING' };

    component.confirmCancelTicket();

    expect(component.ticket.status).toBe('CANCELLED');
    expect(component.effectiveStatus).toBe('CANCELLED');
    expect(component.isCancelling).toBe(false);
    expect(liveTracking.tracking()).toBeNull();
    expect(sessionStorage.getItem('currentTicket')).toBeNull();
  });
});
