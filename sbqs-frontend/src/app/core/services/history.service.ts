import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

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
  private apiUrl = 'http://localhost:8081/api/history';

  getHistory(): Observable<HistoryItem[]> {
    return this.http.get<HistoryItem[]>(this.apiUrl);
  }
}
