import { By } from '@angular/platform-browser';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { ReportService } from '../../../core/services/report.service';
import { ReportExportButtons } from '../../../shared/components/report-export-buttons/report-export-buttons';
import { AdminDashboard } from './admin-dashboard';

describe('AdminDashboard', () => {
  let fixture: ComponentFixture<AdminDashboard>;

  const reportService = {
    export: vi.fn(() => of(new Blob())),
    save: vi.fn(),
  };

  beforeEach(async () => {
    sessionStorage.setItem('fullName', 'Nguyễn Quản Trị');
    vi.clearAllMocks();

    await TestBed.configureTestingModule({
      imports: [AdminDashboard],
      providers: [
        { provide: ReportService, useValue: reportService },
        {
          provide: ApiErrorService,
          useValue: { getMessage: vi.fn((_error: unknown, fallback: string) => fallback) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboard);
    await fixture.whenStable();
  });

  afterEach(() => {
    sessionStorage.removeItem('fullName');
  });

  it('renders the approved page-header hierarchy with real session context', () => {
    const pageHeader: HTMLElement = fixture.nativeElement.querySelector('app-page-header');

    expect(pageHeader.querySelector('h1')?.textContent).toContain('Tổng quan chi nhánh');
    expect(pageHeader.textContent).toContain('Nguyễn Quản Trị');
    expect(pageHeader.querySelector('app-button')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.page-heading')).toBeFalsy();
  });

  it('keeps every existing branch-admin navigation target', () => {
    const links = fixture.nativeElement.querySelectorAll(
      '.dashboard-action',
    ) as NodeListOf<HTMLAnchorElement>;
    const hrefs = Array.from(links).map((link) => link.getAttribute('href'));

    expect(hrefs).toEqual([
      '/admin/operations',
      '/admin/services',
      '/admin/mappings',
      '/admin/users',
      '/admin/history',
    ]);
  });

  it('keeps all four real branch report exports', () => {
    const reportTypes = fixture.debugElement
      .queryAll(By.directive(ReportExportButtons))
      .map((element) => element.componentInstance.reportType);

    expect(reportTypes).toEqual(['users', 'services', 'tickets', 'history']);
  });

  it('uses compact responsive action and report structures without fake KPIs', () => {
    expect(fixture.nativeElement.querySelectorAll('.dashboard-action')).toHaveLength(5);
    expect(fixture.nativeElement.querySelectorAll('.dashboard-report-item')).toHaveLength(4);
    expect(fixture.nativeElement.querySelector('.ui-card-grid')).toBeFalsy();
    expect(fixture.nativeElement.querySelector('.report-center')).toBeFalsy();
    expect(fixture.nativeElement.querySelector('.dashboard-summary-grid')).toBeFalsy();
  });
});
