import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { API_BASE_URL } from '../config/api.config';
import { TicketService } from './ticket.service';

describe('TicketService', () => {
  let service: TicketService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: '/api' },
      ],
    });
    service = TestBed.inject(TicketService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends the same idempotency key supplied by the ticket workflow', () => {
    const key = '123e4567-e89b-12d3-a456-426614174000';

    service.createPreparedTicket(2, 7, { accountNumber: '123' }, key).subscribe();

    const request = http.expectOne('/api/tickets/prepared');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Idempotency-Key')).toBe(key);
    expect(request.request.body).toEqual({
      branchId: 2,
      serviceId: 7,
      values: { accountNumber: '123' },
    });
    request.flush({ ticketId: 10, ticketNumber: 8, status: 'WAITING' });
  });
});
