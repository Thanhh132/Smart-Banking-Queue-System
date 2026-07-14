import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api.config';
import { Service, ServiceCatalogItem } from '../models/service.model';

@Injectable({
  providedIn: 'root'
})
export class AdminServicesService {

  private http = inject(HttpClient);

  private apiUrl = `${inject(API_BASE_URL)}/services`;
  private catalogUrl = `${inject(API_BASE_URL)}/service-catalog`;

  getAllServices() {
    return this.http.get<Service[]>(this.apiUrl);
  }

  getCatalog() { return this.http.get<ServiceCatalogItem[]>(this.catalogUrl); }

  createCatalogItem(payload: { serviceCode: string; serviceName: string; serviceType: string; description: string; estimatedTime: number }) {
    return this.http.post<ServiceCatalogItem>(this.catalogUrl, payload);
  }

  addCatalogItemToBranch(catalogId: number) {
    return this.http.post<Service>(`${this.catalogUrl}/${catalogId}/add-to-branch`, {});
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
