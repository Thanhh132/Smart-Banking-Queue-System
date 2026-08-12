import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { BulkImportService } from '../../../core/services/bulk-import.service';
import { ExcelImportPanel } from './excel-import-panel';

describe('ExcelImportPanel', () => {
  let fixture: ComponentFixture<ExcelImportPanel>;
  const bulkImport = {
    downloadTemplate: vi.fn(() => of(new Blob())),
    saveTemplate: vi.fn(),
    import: vi.fn(() => of({ totalRows: 1, successCount: 1, failureCount: 0, errors: [] })),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [ExcelImportPanel],
      providers: [
        { provide: BulkImportService, useValue: bulkImport },
        { provide: ApiErrorService, useValue: { getMessage: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExcelImportPanel);
    fixture.componentRef.setInput('type', 'staff');
    fixture.detectChanges();
  });

  it('uses shared buttons for template download and import', () => {
    expect(fixture.nativeElement.querySelectorAll('app-button')).toHaveLength(2);
  });

  it('supports an opt-in compact toolbar without changing the default mode', () => {
    expect(fixture.nativeElement.querySelector('.excel-import--compact')).toBeFalsy();

    fixture.componentRef.setInput('compact', true);
    fixture.componentRef.setInput('showTitle', false);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.excel-import--compact')).toBeTruthy();
    expect(fixture.nativeElement.classList).toContain('excel-import-host--compact');
    expect(fixture.nativeElement.querySelector('#excel-import-title')).toBeFalsy();
  });

  it('keeps xlsx validation before calling the import service', () => {
    const file = new File(['invalid'], 'staff.csv', { type: 'text/csv' });
    fixture.componentInstance.selectFile({
      target: { files: [file], value: 'staff.csv' },
    } as unknown as Event);

    expect(fixture.componentInstance.selectedFile).toBeNull();
    expect(fixture.componentInstance.errorMessage).toBeTruthy();
    expect(bulkImport.import).not.toHaveBeenCalled();
  });
});
