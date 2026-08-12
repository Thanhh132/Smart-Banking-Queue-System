import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { HistoryItem, HistoryService } from '../../../core/services/history.service';
import { AdminHistory } from './admin-history';

describe('AdminHistory', () => {
  let component: AdminHistory;
  let fixture: ComponentFixture<AdminHistory>;

  const histories: HistoryItem[] = [
    {
      historyId: 1,
      ticketNumber: 12,
      serviceName: 'Nộp tiền',
      counterName: 'Quầy 01',
      customerEmail: 'customer@sbqs.com',
      staffId: 10,
      staffName: 'Nguyễn Văn An',
      status: 'COMPLETED',
      startedAt: '2026-08-10T08:00:00',
      completedAt: '2026-08-10T08:10:00',
    },
    {
      historyId: 2,
      ticketNumber: 13,
      serviceName: 'Rút tiền',
      counterName: 'Quầy 02',
      customerEmail: 'other@sbqs.com',
      staffId: 11,
      staffName: 'Trần Thị Bình',
      status: 'MISSED',
      startedAt: '2026-08-10T08:15:00',
      completedAt: '2026-08-10T08:20:00',
    },
  ];

  const historyService = {
    getHistory: vi.fn(() => of(histories)),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    historyService.getHistory.mockReturnValue(of(histories));

    await TestBed.configureTestingModule({
      imports: [AdminHistory],
      providers: [
        { provide: HistoryService, useValue: historyService },
        {
          provide: ApiErrorService,
          useValue: { getMessage: vi.fn((_error: unknown, fallback: string) => fallback) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminHistory);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('loads history and renders the read-only table-first structure', () => {
    expect(historyService.getHistory).toHaveBeenCalledOnce();
    expect(component.histories).toEqual(histories);
    expect(fixture.nativeElement.querySelector('app-page-header')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.history-toolbar')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('app-data-table-shell')).toBeTruthy();
    expect(fixture.nativeElement.querySelectorAll('.history-table tbody tr')).toHaveLength(2);
    expect(fixture.nativeElement.querySelector('.history-page button')).toBeFalsy();
  });

  it('keeps the existing staff and status filters auto-applied', async () => {
    component.selectedStaffId = 10;
    component.status = 'COMPLETED';
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();

    expect(component.filteredHistories).toEqual([histories[0]]);
    const tableBody: HTMLElement = fixture.nativeElement.querySelector('.history-table tbody');
    expect(tableBody.textContent).toContain('Nguyễn Văn An');
    expect(tableBody.textContent).not.toContain('Trần Thị Bình');
  });

  it('renders semantic statuses through shared badges', () => {
    const badges: HTMLElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('app-status-badge [data-status]'),
    );

    expect(badges.map((badge) => badge.dataset['status'])).toEqual(['COMPLETED', 'MISSED']);
    expect(fixture.nativeElement.textContent).toContain('Khách không đến');
  });

  it('renders loading state through the shared table shell', async () => {
    const pendingHistory = new Subject<HistoryItem[]>();
    historyService.getHistory.mockReturnValue(pendingHistory);
    const pendingFixture = TestBed.createComponent(AdminHistory);
    pendingFixture.detectChanges();

    expect(pendingFixture.nativeElement.querySelector('app-loading-state')).toBeTruthy();

    pendingHistory.next(histories);
    pendingHistory.complete();
    await pendingFixture.whenStable();
    expect(pendingFixture.nativeElement.querySelector('app-loading-state')).toBeFalsy();
  });

  it('renders a filtered empty state with an actionable message', async () => {
    component.selectedStaffId = 999;
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();

    const emptyState = fixture.nativeElement.querySelector('app-empty-state');
    expect(emptyState).toBeTruthy();
    expect(emptyState.textContent).toContain('Không tìm thấy kết quả');
    expect(emptyState.textContent).toContain('bộ lọc hiện tại');
  });
});
