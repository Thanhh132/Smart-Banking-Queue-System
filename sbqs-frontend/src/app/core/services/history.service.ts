import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';

export interface HistoryItem {
  historyId: number;
  ticketNumber: number;
  serviceName?: string;
  counterName?: string;
  branchName?: string;
  queueMachineName?: string;
  customerEmail?: string;
  staffName?: string;
  status?: string;
  startedAt?: string;
  completedAt?: string;
  staffNote?: string;
}

@Injectable({ providedIn: 'root' })
export class HistoryService {
  private http = inject(HttpClient);
  private apiUrl = `${inject(API_BASE_URL)}/history`;

  getHistory(): Observable<HistoryItem[]> {
    return this.http.get<HistoryItem[]>(this.apiUrl);
  }
}
