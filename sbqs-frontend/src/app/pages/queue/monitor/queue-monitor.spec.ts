import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, Subject, of, throwError } from 'rxjs';

import { QueueMonitor } from '../../../core/models/queue-monitor.model';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { QueueMonitorService } from '../../../core/services/queue-monitor.service';
import { QueueMonitorComponent } from './queue-monitor';

describe('QueueMonitorComponent', () => {
  let component: QueueMonitorComponent;
  let fixture: ComponentFixture<QueueMonitorComponent> | undefined;
  let pollingCallback: (() => void) | undefined;

  const monitor: QueueMonitor = {
    branchName: 'Chi nhánh Trung tâm',
    waitingCount: 7,
    servingCounters: [
      {
        counterName: 'Quầy 01',
        ticketNumber: 105,
        status: 'SERVING',
        queueMachineName: 'Máy sảnh chính',
        staffName: 'Nguyễn Văn An',
      },
      {
        counterName: 'Quầy 02',
        ticketNumber: null,
        status: 'IDLE',
        queueMachineName: 'Máy sảnh chính',
        staffName: 'Trần Thị Bình',
      },
      {
        counterName: 'Quầy 03',
        ticketNumber: null,
        status: 'INACTIVE',
        queueMachineName: null,
        staffName: null,
      },
    ],
  };

  const monitorService = {
    getMonitor: vi.fn<(_branchId: number) => Observable<QueueMonitor>>(() => of(monitor)),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    pollingCallback = undefined;
    monitorService.getMonitor.mockReturnValue(of(monitor));
    vi.spyOn(globalThis, 'setInterval').mockImplementation(((callback: () => void) => {
      pollingCallback = callback;
      return 41;
    }) as typeof setInterval);
    vi.spyOn(globalThis, 'clearInterval').mockImplementation(() => undefined);
    vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('visible');

    await TestBed.configureTestingModule({
      imports: [QueueMonitorComponent],
      providers: [
        { provide: QueueMonitorService, useValue: monitorService },
        {
          provide: ApiErrorService,
          useValue: { getMessage: vi.fn((_error: unknown, fallback: string) => fallback) },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    fixture?.destroy();
    sessionStorage.removeItem('selectedBranchId');
    sessionStorage.removeItem('userRole');
    vi.restoreAllMocks();
  });

  async function createComponent(role: 'BRANCH_ADMIN' | 'STAFF' = 'BRANCH_ADMIN'): Promise<void> {
    sessionStorage.setItem('selectedBranchId', '8');
    sessionStorage.setItem('userRole', role);
    fixture = TestBed.createComponent(QueueMonitorComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  }

  it.each(['BRANCH_ADMIN', 'STAFF'] as const)(
    'loads the same monitor safely for %s',
    async (role) => {
      await createComponent(role);

      expect(monitorService.getMonitor).toHaveBeenCalledWith(8);
      expect(component.monitor).toEqual(monitor);
      expect(fixture!.nativeElement.textContent).toContain('Chi nhánh Trung tâm');
      expect(fixture!.nativeElement.querySelectorAll('.monitor-summary-card')).toHaveLength(4);
    },
  );

  it('calculates compact summaries only from the returned monitor data', async () => {
    await createComponent();

    expect(component.servingCounterCount).toBe(1);
    expect(component.idleCounterCount).toBe(1);
    expect(component.activeCounterCount).toBe(2);
    expect(component.currentlyServingCounters).toEqual([monitor.servingCounters[0]]);
    expect(fixture!.nativeElement.textContent).toContain('7 khách');
    expect(fixture!.nativeElement.textContent).toContain('1 quầy');
  });

  it('renders the current serving ticket and every real counter status', async () => {
    await createComponent();

    expect(fixture!.nativeElement.querySelectorAll('.serving-card')).toHaveLength(1);
    expect(fixture!.nativeElement.querySelector('.serving-item__ticket').textContent).toContain(
      '105',
    );
    expect(fixture!.nativeElement.querySelectorAll('.counter-status-card')).toHaveLength(3);
    expect(fixture!.nativeElement.textContent).toContain('Nguyễn Văn An');
    expect(fixture!.nativeElement.textContent).toContain('Trần Thị Bình');
    expect(fixture!.nativeElement.querySelector('[data-status="INACTIVE"]')).toBeTruthy();
  });

  it('keeps the three-second polling and skips refresh while the tab is hidden', async () => {
    await createComponent();
    expect(setInterval).toHaveBeenCalledWith(expect.any(Function), 3000);

    pollingCallback?.();
    expect(monitorService.getMonitor).toHaveBeenCalledTimes(2);

    vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden');
    pollingCallback?.();
    expect(monitorService.getMonitor).toHaveBeenCalledTimes(2);

    fixture!.destroy();
    fixture = undefined;
    expect(clearInterval).toHaveBeenCalledWith(41);
  });

  it('uses the shared loading state during the initial request', async () => {
    const response = new Subject<QueueMonitor>();
    monitorService.getMonitor.mockReturnValue(response);
    await createComponent();

    expect(fixture!.nativeElement.querySelector('app-loading-state')).toBeTruthy();
    expect(fixture!.nativeElement.querySelector('.monitor-summary-grid')).toBeFalsy();

    response.next(monitor);
    response.complete();
    fixture!.changeDetectorRef.markForCheck();
    await fixture!.whenStable();
    expect(fixture!.nativeElement.querySelector('.monitor-summary-grid')).toBeTruthy();
  });

  it('keeps the existing error message and does not fabricate monitor data', async () => {
    monitorService.getMonitor.mockReturnValue(throwError(() => new Error('offline')));
    await createComponent('STAFF');

    expect(component.monitor).toBeNull();
    expect(component.errorMessage).toBe('Không tải được dữ liệu màn hình hàng đợi.');
    expect(fixture!.nativeElement.querySelector('[role="alert"]')).toBeTruthy();
    expect(fixture!.nativeElement.querySelector('.monitor-summary-grid')).toBeFalsy();
  });

  it('shows compact shared empty states when no counter is configured or serving', async () => {
    monitorService.getMonitor.mockReturnValue(
      of({ branchName: 'Chi nhánh mới', waitingCount: 0, servingCounters: [] }),
    );
    await createComponent();

    expect(fixture!.nativeElement.querySelectorAll('app-empty-state')).toHaveLength(2);
    expect(fixture!.nativeElement.textContent).toContain('Chưa có quầy đang phục vụ');
    expect(fixture!.nativeElement.textContent).toContain('Chưa có quầy giao dịch');
  });
});
