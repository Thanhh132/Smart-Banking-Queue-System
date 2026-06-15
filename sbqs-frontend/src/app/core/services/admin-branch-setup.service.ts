import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class AdminBranchSetupService {

  private http = inject(HttpClient);

  private branchApi = 'http://localhost:8081/api/branches';
  private queueMachineApi = 'http://localhost:8081/api/queue-machines';
  private servicesApi = 'http://localhost:8081/api/services';
  private countersApi = 'http://localhost:8081/api/counters';
  private mappingsApi = 'http://localhost:8081/api/queue-machine-mappings';

  getBranch(branchId: number) {
    return this.http.get<any[]>(this.branchApi);
  }

  getQueueMachines() {
    return this.http.get<any[]>(this.queueMachineApi);
  }

  getServicesByBranch(branchId: number) {
    return this.http.get<any[]>(
      `${this.servicesApi}?branchId=${branchId}`
    );
  }

  getCountersByBranch(branchId: number) {
    return this.http.get<any[]>(
      `${this.countersApi}?branchId=${branchId}`
    );
  }

  getMappings() {
    return this.http.get<any[]>(this.mappingsApi);
  }

  createQueueMachine(machine: any) {
    return this.http.post(
      this.queueMachineApi,
      machine
    );
  }

  createService(service: any) {
    return this.http.post(
      this.servicesApi,
      service
    );
  }

  createMapping(
    queueMachineId: number,
    serviceId: number
  ) {
    return this.http.post(
      this.mappingsApi,
      {
        queueMachineId,
        serviceId
      }
    );
  }
}