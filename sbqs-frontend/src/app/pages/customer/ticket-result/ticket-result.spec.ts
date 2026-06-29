import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { HistoryService } from '../../../core/services/history.service';
import { QueueMonitorService } from '../../../core/services/queue-monitor.service';
import { TicketService } from '../../../core/services/ticket.service';
import { TicketResult } from './ticket-result';

describe('TicketResult', () => {
  let component: TicketResult;
  let fixture: ComponentFixture<TicketResult>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TicketResult],
      providers: [
        provideRouter([]),
        { provide: HistoryService, useValue: { getHistory: () => of([]) } },
        { provide: QueueMonitorService, useValue: { getMonitor: () => of(null) } },
        { provide: TicketService, useValue: { getCurrentTicket: () => of(null) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TicketResult);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
