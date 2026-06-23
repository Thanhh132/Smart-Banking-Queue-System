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

  getPendingApprovalTasks() {
    return this.http.get<any[]>(
      'http://localhost:8081/api/workflows/tickets/pending-approval'
    );
  }

  getCounters() {
    const branchId = Number(localStorage.getItem('selectedBranchId'));
    const url = branchId
      ? `http://localhost:8081/api/counters?branchId=${branchId}`
      : 'http://localhost:8081/api/counters';

    return this.http.get<any[]>(
      url
    );
  }

  getAssignedCounter() {
    return this.http.get<any>(
      'http://localhost:8081/api/counters/assigned'
    );
  }

  assignCounter(counterId: number) {
    return this.http.post(
      `http://localhost:8081/api/counters/${counterId}/assign`,
      {}
    );
  }

  unassignCounter(counterId: number) {
    return this.http.post(
      `http://localhost:8081/api/counters/${counterId}/unassign`,
      {}
    );
  }
}
