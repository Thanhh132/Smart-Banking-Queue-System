import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';

export interface QueueMachinePayload {
  machineCode: string;
  machineName: string;
  locationNote?: string;
  instructionNote?: string;
  status: string;
  branch: {
    branchId: number;
  };
}

export interface CounterPayload {
  counterCode: string;
  counterName: string;
  status: string;
  branch: {
    branchId: number;
  };
  queueMachine?: {
    queueMachineId: number;
  } | null;
}

@Injectable({
  providedIn: 'root',
})
export class AdminOperationsService {
  private http = inject(HttpClient);
  private apiBaseUrl = inject(API_BASE_URL);

  private queueMachineApi = `${this.apiBaseUrl}/queue-machines`;
  private counterApi = `${this.apiBaseUrl}/counters`;

  getQueueMachines(): Observable<any[]> {
    return this.http.get<any[]>(this.queueMachineApi);
  }

  createQueueMachine(payload: QueueMachinePayload): Observable<any> {
    return this.http.post<any>(this.queueMachineApi, payload);
  }

  updateQueueMachine(id: number, payload: QueueMachinePayload): Observable<any> {
    return this.http.put<any>(`${this.queueMachineApi}/${id}`, payload);
  }

  deleteQueueMachine(id: number): Observable<string> {
    return this.http.delete(`${this.queueMachineApi}/${id}`, {
      responseType: 'text',
    });
  }

  getCounters(branchId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.counterApi}?branchId=${branchId}`);
  }

  createCounter(payload: CounterPayload): Observable<any> {
    return this.http.post<any>(this.counterApi, payload);
  }

  updateCounter(id: number, payload: CounterPayload): Observable<any> {
    return this.http.put<any>(`${this.counterApi}/${id}`, payload);
  }

  deleteCounter(id: number): Observable<string> {
    return this.http.delete(`${this.counterApi}/${id}`, {
      responseType: 'text',
    });
  }
}
