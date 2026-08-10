import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { ReportService } from '../../../core/services/report.service';
import { ReportExportButtons } from './report-export-buttons';

describe('ReportExportButtons', () => {
  let fixture: ComponentFixture<ReportExportButtons>;
  const reportService = {
    export: vi.fn(() => of(new Blob())),
    save: vi.fn(),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [ReportExportButtons],
      providers: [
        { provide: ReportService, useValue: reportService },
        { provide: ApiErrorService, useValue: { getMessage: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportExportButtons);
    fixture.componentRef.setInput('reportType', 'history');
    fixture.detectChanges();
  });

  it('renders compact shared export buttons', () => {
    expect(fixture.nativeElement.querySelectorAll('app-button')).toHaveLength(2);
  });

  it('keeps the existing report export workflow', () => {
    fixture.componentInstance.export('pdf');

    expect(reportService.export).toHaveBeenCalledWith('history', 'pdf');
    expect(reportService.save).toHaveBeenCalled();
  });
});
