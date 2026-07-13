import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface TicketTracking {
  ticketId: number;
  ticketNumber: number;
  status: 'WAITING' | 'SERVING' | 'COMPLETED' | 'CANCELLED';
  peopleAhead: number;
  counterName: string | null;
  branchName: string | null;
  serviceName: string | null;
  queueMachineId: number | null;
  queueMachineLocationNote: string | null;
  servingStartedAt: string | null;
}

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

  createPreparedTicket(branchId: number, serviceId: number, values: Record<string, unknown>) {
    return this.http.post(`${this.apiUrl}/prepared`, { branchId, serviceId, values });
  }

  cancelTicket(ticketId: number) {
    return this.http.post(`${this.apiUrl}/${ticketId}/cancel`, {});
  }

  getCurrentTicket() {
    return this.http.get<any>(`${this.apiUrl}/current`);
  }

  getTracking(ticketId: number) {
    return this.http.get<TicketTracking>(`${this.apiUrl}/${ticketId}/tracking`);
  }
}
