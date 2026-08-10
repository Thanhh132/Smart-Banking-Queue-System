import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { CustomerLiveTrackingService } from './customer-live-tracking.service';
import { TicketService, TicketTracking } from './ticket.service';

describe('CustomerLiveTrackingService', () => {
  let service: CustomerLiveTrackingService;

  beforeEach(() => {
    sessionStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        {
          provide: TicketService,
          useValue: {
            getCurrentTicket: () => of(null),
            getTracking: () => of(null),
          },
        },
      ],
    });
    service = TestBed.inject(CustomerLiveTrackingService);
  });

  it('notifies the customer when their ticket is near', () => {
    applyTracking(tracking({ status: 'WAITING', peopleAhead: 3 }));

    expect(service.notice()?.title).toBe('Sắp đến lượt bạn');
    expect(service.notice()?.message).toContain('3 phiếu');
  });

  it('replaces the near notice when the ticket is called', () => {
    applyTracking(tracking({ status: 'WAITING', peopleAhead: 1 }));
    applyTracking(tracking({ status: 'SERVING', peopleAhead: 0, counterName: 'Quầy 202' }));

    expect(service.notice()?.title).toBe('Đã đến lượt bạn');
    expect(service.notice()?.message).toContain('Quầy 202');
  });

  it('resets notification state when tracking a new ticket', () => {
    applyTracking(tracking({ ticketId: 10, status: 'SERVING' }));
    service.dismissNotice();
    applyTracking(tracking({ ticketId: 11, status: 'WAITING', peopleAhead: 0 }));

    expect(service.notice()?.title).toBe('Sắp đến lượt bạn');
  });

  it('clears customer tracking state after the last consumer stops', () => {
    service.start();
    applyTracking(tracking({ status: 'WAITING', peopleAhead: 0 }));

    service.stop();

    expect(service.notice()).toBeNull();
    expect(service.tracking()).toBeNull();
    expect(service.lastUpdatedAt()).toBeNull();
  });

  it('does not repeat a cancelled notice after leaving and returning to a customer page', () => {
    applyTracking(tracking({ status: 'CANCELLED', peopleAhead: 0 }));
    expect(service.notice()?.title).toBe('Phiếu đã hủy');

    service.start();
    service.stop();
    applyTracking(tracking({ status: 'CANCELLED', peopleAhead: 0 }));

    expect(service.notice()).toBeNull();
    expect(sessionStorage.getItem('currentTicket')).toBeNull();
  });

  function applyTracking(value: TicketTracking): void {
    (service as any).applyTracking(value);
  }

  function tracking(overrides: Partial<TicketTracking> = {}): TicketTracking {
    return {
      ticketId: 10,
      ticketNumber: 7,
      status: 'WAITING',
      peopleAhead: 4,
      counterName: null,
      branchName: 'SBQS Thủ Dầu Một',
      serviceName: 'Chuyển khoản',
      queueMachineId: 1,
      queueMachineLocationNote: 'Tầng 2',
      servingStartedAt: null,
      ...overrides,
    };
  }
});
