import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';

import {
  BulkImportService,
  ImportResult,
  ImportType,
} from '../../../core/services/bulk-import.service';
import { ApiErrorService } from '../../../core/services/api-error.service';

@Component({
  selector: 'app-excel-import-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './excel-import-panel.html',
  styleUrl: './excel-import-panel.scss',
})
export class ExcelImportPanel {
  private bulkImportService = inject(BulkImportService);
  private apiError = inject(ApiErrorService);

  @Input({ required: true }) type!: ImportType;
  @Input() title = 'Nhập dữ liệu từ Excel';
  @Output() imported = new EventEmitter<ImportResult>();

  selectedFile: File | null = null;
  isDownloading = false;
  isImporting = false;
  errorMessage = '';
  result: ImportResult | null = null;

  get selectedFileName(): string {
    return this.selectedFile?.name || 'Chưa chọn file';
  }

  get visibleErrors() {
    return this.result?.errors.slice(0, 8) || [];
  }

  selectFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] || null;
    this.result = null;
    this.errorMessage = '';

    if (file && !file.name.toLowerCase().endsWith('.xlsx')) {
      this.selectedFile = null;
      this.errorMessage = 'Chỉ hỗ trợ file Excel định dạng .xlsx.';
      input.value = '';
      return;
    }
    if (file && file.size > 5 * 1024 * 1024) {
      this.selectedFile = null;
      this.errorMessage = 'File Excel không được lớn hơn 5 MB.';
      input.value = '';
      return;
    }

    this.selectedFile = file;
  }

  downloadTemplate(): void {
    this.isDownloading = true;
    this.errorMessage = '';
    this.bulkImportService.downloadTemplate(this.type).subscribe({
      next: (blob) => {
        this.bulkImportService.saveTemplate(this.type, blob);
        this.isDownloading = false;
      },
      error: (error) => {
        this.errorMessage = this.apiError.getMessage(error, 'Không tải được file Excel mẫu.');
        this.isDownloading = false;
      },
    });
  }

  submit(): void {
    if (!this.selectedFile) {
      this.errorMessage = 'Vui lòng chọn file Excel cần nhập.';
      return;
    }

    this.isImporting = true;
    this.result = null;
    this.errorMessage = '';
    this.bulkImportService.import(this.type, this.selectedFile).subscribe({
      next: (result) => {
        this.result = result;
        this.isImporting = false;
        this.imported.emit(result);
      },
      error: (error) => {
        this.errorMessage = this.apiError.getMessage(error, 'Không thể nhập dữ liệu từ file Excel.');
        this.isImporting = false;
      },
    });
  }
}
