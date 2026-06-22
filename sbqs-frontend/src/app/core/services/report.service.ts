import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export type ReportType = 'users' | 'services' | 'tickets';
export type ReportFormat = 'pdf' | 'xlsx';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8081/api/reports';

  export(reportType: ReportType, format: ReportFormat) {
    return this.http.get(`${this.apiUrl}/${reportType}`, {
      params: { format },
      responseType: 'blob',
    });
  }

  save(blob: Blob, reportType: ReportType, format: ReportFormat): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `sbqs-${reportType}-report.${format}`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
}
