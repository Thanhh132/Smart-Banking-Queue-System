import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { StaffService } from '../../../core/services/staff.service';
import { StaffDashboard } from './staff-dashboard';

describe('StaffDashboard', () => {
  let component: StaffDashboard;
  let fixture: ComponentFixture<StaffDashboard> | undefined;

  const counter = {
    counterId: 3,
    counterName: 'Quầy 03',
    status: 'ACTIVE',
    queueMachine: { machineName: 'Máy sảnh chính' },
    currentTicket: null,
  };
  const waitingCounter = {
    counterId: 4,
    counterName: 'Quầy 04',
    status: 'INACTIVE',
    queueMachine: { machineName: 'Máy sảnh phụ' },
  };
  const ticket = {
    ticketId: 21,
    ticketNumber: 108,
    status: 'SERVING',
    servingStartedAt: '2026-08-11T07:00:00Z',
    service: { serviceName: 'Nộp tiền' },
    hasPaperlessProfile: true,
    paperlessFields: [{ label: 'Số tài khoản', value: '0123456789' }],
  };
  const task = {
    ticketNumber: 109,
    status: 'WAITING',
    serviceName: 'Rút tiền',
    queueMachineName: 'Máy sảnh chính',
    customerEmail: 'customer@sbqs.vn',
  };
  const staffService = {
    getCounters: vi.fn(() => of([counter, waitingCounter])),
    getAssignedCounter: vi.fn<() => Observable<any>>(() => of(counter)),
    getPendingApprovalTasks: vi.fn(() => of([task])),
    getTicketStaffView: vi.fn(() => of(ticket)),
    assignCounter: vi.fn(() => of(counter)),
    unassignCounter: vi.fn(() => of({})),
    callNext: vi.fn(() => of(ticket)),
    complete: vi.fn(() => of({})),
    markNoShow: vi.fn(() => of({})),
    verifyDelegation: vi.fn(() => of({ delegationId: 8, ownerName: 'Nguyễn Văn A' })),
    markDelegationUsed: vi.fn(() => of({})),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    staffService.getCounters.mockReturnValue(of([counter, waitingCounter]));
    staffService.getAssignedCounter.mockReturnValue(of(counter));
    staffService.getPendingApprovalTasks.mockReturnValue(of([task]));
    staffService.getTicketStaffView.mockReturnValue(of(ticket));
    vi.spyOn(globalThis, 'setInterval').mockImplementation((() => 72) as typeof setInterval);
    vi.spyOn(globalThis, 'clearInterval').mockImplementation(() => undefined);

    await TestBed.configureTestingModule({
      imports: [StaffDashboard],
      providers: [
        { provide: StaffService, useValue: staffService },
        {
          provide: ApiErrorService,
          useValue: { getMessage: vi.fn((_error: unknown, fallback: string) => fallback) },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    fixture?.destroy();
    fixture = undefined;
    vi.restoreAllMocks();
  });

  async function createComponent(assignedCounter: any = counter): Promise<void> {
    staffService.getAssignedCounter.mockReturnValue(of(assignedCounter));
    fixture = TestBed.createComponent(StaffDashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  }

  it('renders the operational hierarchy with shared UI primitives and real counts', async () => {
    await createComponent();

    const text = fixture!.nativeElement.textContent;
    expect(fixture!.nativeElement.querySelector('app-page-header')).toBeTruthy();
    expect(fixture!.nativeElement.querySelector('.staff-current-ticket')).toBeTruthy();
    expect(fixture!.nativeElement.querySelector('app-data-table-shell')).toBeFalsy();
    expect(text).toContain('Quầy 03');
    expect(text).toContain('1 khách');
  });

  it('prioritizes counter selection when the staff member has no active session', async () => {
    await createComponent(null);

    expect(fixture!.nativeElement.querySelector('.staff-counter-selection')).toBeTruthy();
    expect(fixture!.nativeElement.textContent).toContain('Chưa bắt đầu ca làm việc');

    component.selectedCounterId = 4;
    component.assignCounter();
    expect(staffService.assignCounter).toHaveBeenCalledWith(4);
  });

  it('calls the next ticket only from an assigned and idle counter', async () => {
    await createComponent();

    component.callNext();
    expect(staffService.callNext).toHaveBeenCalledWith(3);
    expect(component.currentTicket).toEqual(ticket);
  });

  it('shows the current ticket and preserves the complete workflow', async () => {
    staffService.getAssignedCounter.mockReturnValue(of({ ...counter, currentTicket: ticket }));
    await createComponent({ ...counter, currentTicket: ticket });

    expect(fixture!.nativeElement.querySelector('.staff-ticket__number').textContent).toContain('108');
    expect(fixture!.nativeElement.querySelector('.staff-prepared-information')).toBeTruthy();

    component.complete();
    expect(staffService.complete).toHaveBeenCalledWith(21);
  });

  it('requires the shared confirmation dialog before marking a customer as absent', async () => {
    await createComponent({ ...counter, currentTicket: ticket });

    component.markNoShow();
    fixture!.changeDetectorRef.markForCheck();
    await fixture!.whenStable();

    expect(staffService.markNoShow).not.toHaveBeenCalled();
    expect(fixture!.nativeElement.querySelector('app-confirm-dialog [role="dialog"]')).toBeTruthy();

    component.confirmMarkNoShow();
    expect(staffService.markNoShow).toHaveBeenCalledWith(21);
  });

  it('preserves delegation verification and acceptance calls', async () => {
    await createComponent();
    component.delegationCode = 'UQ-12345678';
    component.delegationIdentity = '012345678901';

    component.verifyDelegation();
    expect(staffService.verifyDelegation).toHaveBeenCalledWith('UQ-12345678', '012345678901');

    component.acceptDelegation();
    expect(staffService.markDelegationUsed).toHaveBeenCalledWith(8);
  });
});
