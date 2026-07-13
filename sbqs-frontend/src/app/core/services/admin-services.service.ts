import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class AdminServicesService {

  private http = inject(HttpClient);

  private apiUrl = 'http://localhost:8081/api/services';

  getAllServices() {
    return this.http.get<any[]>(this.apiUrl);
  }

  getServicesByBranch(branchId: number) {
    return this.http.get<any[]>(`${this.apiUrl}?branchId=${branchId}`);
  }

  createService(payload: any) {
    return this.http.post(this.apiUrl, payload);
  }

  updateService(serviceId: number, payload: any) {
    return this.http.put(`${this.apiUrl}/${serviceId}`, payload);
  }

  deleteService(serviceId: number) {
    return this.http.delete(
      `${this.apiUrl}/${serviceId}`
    );
  }

}
