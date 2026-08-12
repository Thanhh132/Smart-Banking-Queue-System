import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api.config';

export interface StaffTicketView {
  ticketId: number;
  ticketNumber: number;
  status: string;
  customerEmail: string | null;
  servingStartedAt: string | null;
  customer: { userId: number; fullName: string; email: string; phone: string } | null;
  service: { serviceId: number; serviceCode: string; serviceName: string; serviceType: string } | null;
  paperlessFields: { key: string; label: string; value: string }[];
  hasPaperlessProfile: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class StaffService {

  private http = inject(HttpClient);
  private apiBaseUrl = inject(API_BASE_URL);

  private apiUrl = `${this.apiBaseUrl}/tickets`;

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

  markNoShow(ticketId: number) {
    return this.http.post(`${this.apiUrl}/${ticketId}/no-show`, {});
  }

  getTicketStaffView(ticketId: number) {
    return this.http.get<StaffTicketView>(
      `${this.apiUrl}/${ticketId}/staff-view`
    );
  }

  getPendingApprovalTasks() {
    return this.http.get<any[]>(
      `${this.apiBaseUrl}/workflows/tickets/pending-approval`
    );
  }

  getCounters() {
    const branchId = Number(sessionStorage.getItem('selectedBranchId'));
    const url = branchId
      ? `${this.apiBaseUrl}/counters?branchId=${branchId}`
      : `${this.apiBaseUrl}/counters`;

    return this.http.get<any[]>(
      url
    );
  }

  getAssignedCounter() {
    return this.http.get<any>(
      `${this.apiBaseUrl}/counters/assigned`
    );
  }

  assignCounter(counterId: number) {
    return this.http.post(
      `${this.apiBaseUrl}/counters/${counterId}/assign`,
      {}
    );
  }

  unassignCounter(counterId: number) {
    return this.http.post(
      `${this.apiBaseUrl}/counters/${counterId}/unassign`,
      {}
    );
  }

  verifyDelegation(referenceCode: string, delegateIdentityNumber: string) {
    return this.http.post<any>(`${this.apiBaseUrl}/delegations/verify`, { referenceCode, delegateIdentityNumber });
  }

  markDelegationUsed(delegationId: number) {
    return this.http.post<any>(`${this.apiBaseUrl}/delegations/${delegationId}/use`, {});
  }
}
