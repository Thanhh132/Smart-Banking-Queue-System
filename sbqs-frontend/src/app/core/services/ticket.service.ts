import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class TicketService {

  private http = inject(HttpClient);

  private apiUrl =
    'http://localhost:8081/api/tickets';

  createTicket(
    branchId: number,
    serviceId: number
  ) {

    const payload = {
      branch: {
        branchId: branchId
      },
      service: {
        serviceId: serviceId
      }
    };

    return this.http.post(
      this.apiUrl,
      payload
    );
  }

  cancelTicket(ticketId: number) {
    return this.http.post(`${this.apiUrl}/${ticketId}/cancel`, {});
  }

  getCurrentTicket() {
    return this.http.get<any>(`${this.apiUrl}/current`);
  }
}
