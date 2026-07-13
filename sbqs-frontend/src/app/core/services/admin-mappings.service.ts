import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class AdminMappingsService {

  private http = inject(HttpClient);
  private apiBaseUrl = inject(API_BASE_URL);

  private queueMachineApi =
    `${this.apiBaseUrl}/queue-machines`;

  private servicesApi =
    `${this.apiBaseUrl}/services`;

  private mappingsApi =
    `${this.apiBaseUrl}/queue-machine-mappings`;

  getQueueMachines() {
    return this.http.get<any[]>(this.queueMachineApi);
  }

  getServices(branchId?: number) {
    const url = branchId ? `${this.servicesApi}?branchId=${branchId}` : this.servicesApi;
    return this.http.get<any[]>(url);
  }

  getMappings() {
    return this.http.get<any[]>(this.mappingsApi);
  }

  createMapping(queueMachineId: number, serviceId: number) {
    return this.http.post(this.mappingsApi, {
      queueMachineId,
      serviceId
    });
  }

  deleteMapping(queueMachineId: number, serviceId: number) {
    return this.http.delete(
      this.mappingsApi,
      {
        body: {
          queueMachineId,
          serviceId
        },
        responseType: 'text'
      }
    );
  }
}

