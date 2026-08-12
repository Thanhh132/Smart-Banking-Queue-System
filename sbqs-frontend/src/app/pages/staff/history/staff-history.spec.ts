import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { HistoryService } from '../../../core/services/history.service';
import { StaffHistory } from './staff-history';

describe('StaffHistory', () => {
  it('renders the current staff history in the shared table pattern', async () => {
    await TestBed.configureTestingModule({
      imports: [StaffHistory],
      providers: [
        {
          provide: HistoryService,
          useValue: {
            getHistory: () =>
              of([
                {
                  historyId: 1,
                  ticketNumber: 101,
                  serviceName: 'Nộp tiền',
                  counterName: 'Quầy 01',
                  status: 'COMPLETED',
                },
              ]),
          },
        },
        { provide: ApiErrorService, useValue: { getMessage: (_: unknown, fallback: string) => fallback } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(StaffHistory);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('app-page-header')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('app-data-table-shell')).toBeTruthy();
    expect(fixture.nativeElement.querySelectorAll('.sbqs-metric')).toHaveLength(3);
    expect(fixture.nativeElement.textContent).toContain('Nộp tiền');
  });
});
