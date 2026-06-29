import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, Input, inject } from '@angular/core';
import { finalize } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { ReportFormat, ReportService, ReportType } from '../../../core/services/report.service';
import { AppIcon } from '../app-icon/app-icon';

@Component({
  selector: 'app-report-export-buttons',
  standalone: true,
  imports: [CommonModule, AppIcon],
  templateUrl: './report-export-buttons.html',
  styleUrl: './report-export-buttons.scss',
})
export class ReportExportButtons {
  private reportService = inject(ReportService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  @Input({ required: true }) reportType!: ReportType;
  loadingFormat: ReportFormat | null = null;
  errorMessage = '';

  export(format: ReportFormat): void {
    this.loadingFormat = format;
    this.errorMessage = '';
    this.reportService.export(this.reportType, format)
      .pipe(finalize(() => {
        this.loadingFormat = null;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (blob) => this.reportService.save(blob, this.reportType, format),
        error: (err) => {
          this.errorMessage = this.apiError.getMessage(err, 'Không xuất được báo cáo.');
          this.cdr.detectChanges();
        },
      });
  }
}
