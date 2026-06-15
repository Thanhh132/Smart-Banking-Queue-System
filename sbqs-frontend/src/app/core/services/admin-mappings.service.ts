import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class AdminMappingsService {

  private http = inject(HttpClient);

  private queueMachineApi =
    'http://localhost:8081/api/queue-machines';

  private servicesApi =
    'http://localhost:8081/api/services';

  private mappingsApi =
    'http://localhost:8081/api/queue-machine-mappings';

  getQueueMachines() {
    return this.http.get<any[]>(this.queueMachineApi);
  }

  getServices() {
    return this.http.get<any[]>(this.servicesApi);
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
    return this.http.request(
      'delete',
      this.mappingsApi,
      {
        body: {
          queueMachineId,
          serviceId
        }
      }
    );
  }
}