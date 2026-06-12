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

  createService(service: any) {
    return this.http.post(this.apiUrl, service);
  }

  deleteService(id: number) {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}