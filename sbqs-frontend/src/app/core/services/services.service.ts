import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Service } from '../models/service.model';

@Injectable({
  providedIn: 'root'
})
export class ServicesService {

  private http = inject(HttpClient);

  private apiUrl = 'http://localhost:8081/api/services';

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
