import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../config/api.config';

export type ImportType = 'staff' | 'services';

export interface ImportRowError {
  row: number;
  identifier: string;
  message: string;
}

export interface ImportResult {
  totalRows: number;
  successCount: number;
  failureCount: number;
  errors: ImportRowError[];
}

@Injectable({ providedIn: 'root' })
export class BulkImportService {
  private http = inject(HttpClient);
  private apiUrl = `${inject(API_BASE_URL)}/import`;

  downloadTemplate(type: ImportType) {
    return this.http.get(`${this.apiUrl}/templates/${type}`, { responseType: 'blob' });
  }

  import(type: ImportType, file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ImportResult>(`${this.apiUrl}/${type}`, formData);
  }

  saveTemplate(type: ImportType, blob: Blob): void {
    const fileName = type === 'staff'
      ? 'sbqs-staff-import-template.xlsx'
      : 'sbqs-services-import-template.xlsx';
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    URL.revokeObjectURL(url);
  }
}
