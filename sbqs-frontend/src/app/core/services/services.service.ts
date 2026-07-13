import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Service } from '../models/service.model';
import { API_BASE_URL } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class ServicesService {

  private http = inject(HttpClient);

  private apiUrl = `${inject(API_BASE_URL)}/services`;

  getServicesByBranch(branchId: number): Observable<Service[]> {
    return this.http.get<Service[]>(
      `${this.apiUrl}?branchId=${branchId}`
    );
  }

  getMappedServicesByBranch(branchId: number): Observable<Service[]> {
    return this.http.get<Service[]>(
      `${this.apiUrl}?branchId=${branchId}&mappedOnly=true`
    );
  }

  getAllServices(): Observable<Service[]> {
    return this.http.get<Service[]>(this.apiUrl);
  }
}
