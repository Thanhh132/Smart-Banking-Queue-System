import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api.config';

export interface Delegation {
  delegationId: number; referenceCode: string; delegateName: string; maskedIdentity: string;
  delegateDateOfBirth?: string; delegatePhone?: string; identityIssueDate?: string; identityExpiryDate?: string; identityIssuePlace?: string;
  relationship: string; transactionScope: string; status: string;
  ownerName: string; maskedOwnerEmail: string;
  branchId: number; branchName: string; serviceId: number; serviceName: string;
  validFrom: string; validUntil: string; createdAt: string; verifiedAt?: string; usedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class DelegationService {
  private http = inject(HttpClient);
  private apiUrl = `${inject(API_BASE_URL)}/delegations`;
  getMine() { return this.http.get<Delegation[]>(`${this.apiUrl}/mine`); }
  create(payload: any) { return this.http.post<Delegation>(this.apiUrl, payload); }
  cancel(id: number) { return this.http.post<Delegation>(`${this.apiUrl}/${id}/cancel`, {}); }
  verify(referenceCode: string, delegateIdentityNumber: string) {
    return this.http.post<Delegation>(`${this.apiUrl}/verify`, { referenceCode, delegateIdentityNumber });
  }
  markUsed(id: number) { return this.http.post<Delegation>(`${this.apiUrl}/${id}/use`, {}); }
}
