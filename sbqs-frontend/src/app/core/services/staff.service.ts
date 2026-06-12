import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class StaffService {

  private http = inject(HttpClient);

  private apiUrl =
    'http://localhost:8081/api/tickets';

  callNext(counterId: number) {
    return this.http.post(
      `${this.apiUrl}/call-next?counterId=${counterId}`,
      {}
    );
  }

  complete(ticketId: number) {
    return this.http.post(
      `${this.apiUrl}/${ticketId}/complete`,
      {}
    );
  }

  getCounters() {
    return this.http.get<any[]>(
      'http://localhost:8081/api/counters'
    );
  }
}