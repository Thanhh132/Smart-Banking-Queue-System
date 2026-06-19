import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Branch } from '../models/branch.model';

@Injectable({
  providedIn: 'root'
})
export class BranchService {

  private http = inject(HttpClient);

  private apiUrl =
    'http://localhost:8081/api/branches';

  getBranches(): Observable<Branch[]> {
    return this.http.get<Branch[]>(this.apiUrl);
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
