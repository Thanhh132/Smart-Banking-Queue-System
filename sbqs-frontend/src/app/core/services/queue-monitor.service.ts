import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { QueueMonitor } from '../models/queue-monitor.model';
import { API_BASE_URL } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class QueueMonitorService {

  private http = inject(HttpClient);

  private apiUrl = `${inject(API_BASE_URL)}/queue-monitor`;

  getMonitor(
    branchId: number,
    queueMachineId?: number | null
  ): Observable<QueueMonitor> {
    const queueMachineParam = queueMachineId
      ? `&queueMachineId=${queueMachineId}`
      : '';

    return this.http.get<QueueMonitor>(
      `${this.apiUrl}?branchId=${branchId}${queueMachineParam}`
    );
  }
}
