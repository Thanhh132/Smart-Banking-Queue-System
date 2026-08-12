import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Customer } from './customer';

describe('Customer portal', () => {
  afterEach(() => sessionStorage.removeItem('currentTicket'));

  it('prioritizes the active ticket journey when one exists', async () => {
    sessionStorage.setItem('currentTicket', JSON.stringify({ ticketNumber: 108, status: 'WAITING', serviceName: 'Nộp tiền' }));
    await TestBed.configureTestingModule({ imports: [Customer], providers: [provideRouter([])] }).compileComponents();
    const fixture = TestBed.createComponent(Customer);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.customer-current-journey')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('#108');
    expect(fixture.nativeElement.textContent).toContain('Theo dõi phiếu');
  });
});
