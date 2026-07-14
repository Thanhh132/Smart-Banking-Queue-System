import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Branch, SmartBranchRecommendation } from '../models/branch.model';
import { API_BASE_URL } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class BranchService {

  private http = inject(HttpClient);

  private apiUrl = `${inject(API_BASE_URL)}/branches`;

  getBranches(): Observable<Branch[]> {
    return this.http.get<Branch[]>(this.apiUrl);
  }

  getSmartRecommendations(bankName: string, latitude: number, longitude: number, serviceCode?: string) {
    let params = new HttpParams()
      .set('bankName', bankName)
      .set('latitude', latitude)
      .set('longitude', longitude);
    if (serviceCode) params = params.set('serviceCode', serviceCode);
    return this.http.get<SmartBranchRecommendation[]>(`${this.apiUrl}/recommendations`, { params });
  }

  createBranch(payload: any): Observable<Branch> {
    return this.http.post<Branch>(this.apiUrl, payload);
  }

  updateBranch(branchId: number, payload: any): Observable<Branch> {
    return this.http.put<Branch>(`${this.apiUrl}/${branchId}`, payload);
  }

  deleteBranch(branchId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${branchId}`);
  }
}
