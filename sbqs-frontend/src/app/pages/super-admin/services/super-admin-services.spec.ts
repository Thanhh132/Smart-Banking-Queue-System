import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ServiceCatalogItem } from '../../../core/models/service.model';
import { AdminServicesService } from '../../../core/services/admin-services.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { BulkImportService } from '../../../core/services/bulk-import.service';
import { SuperAdminServices } from './super-admin-services';

describe('SuperAdminServices', () => {
  let fixture: ComponentFixture<SuperAdminServices>;
  let component: SuperAdminServices;

  const active: ServiceCatalogItem = {
    catalogId: 1,
    serviceCode: 'CARD',
    serviceName: 'Phát hành thẻ',
    serviceType: 'THẺ',
    description: 'Phát hành thẻ vật lý',
    estimatedTime: 15,
    status: 'ACTIVE',
    delegatable: true,
  };
  const archived: ServiceCatalogItem = {
    ...active,
    catalogId: 2,
    serviceCode: 'LEGACY',
    serviceName: 'Dịch vụ cũ',
    status: 'ARCHIVED',
    delegatable: false,
  };

  const api = {
    getCatalog: vi.fn(() => of([active, archived])),
    createCatalogItem: vi.fn((payload: object) => of({ ...active, ...payload, catalogId: 3 })),
    updateCatalogItem: vi.fn((id: number, payload: object) =>
      of({ ...active, ...payload, catalogId: id }),
    ),
    deleteCatalogItem: vi.fn(() => of(void 0)),
    restoreCatalogItem: vi.fn(() => of(active)),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    api.getCatalog.mockReturnValue(of([active, archived]));

    await TestBed.configureTestingModule({
      imports: [SuperAdminServices],
      providers: [
        { provide: AdminServicesService, useValue: api },
        { provide: BulkImportService, useValue: {} },
        {
          provide: ApiErrorService,
          useValue: { getMessage: vi.fn((_error: unknown, fallback: string) => fallback) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SuperAdminServices);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('renders a table-first catalog with shared primitives and real metrics', () => {
    expect(api.getCatalog).toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('app-page-header')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('app-data-table-shell')).toBeTruthy();
    expect(fixture.nativeElement.querySelectorAll('.sbqs-metric')).toHaveLength(3);
    expect(fixture.nativeElement.querySelectorAll('app-status-badge').length).toBeGreaterThan(0);
    expect(fixture.nativeElement.textContent).toContain('2 dịch vụ');
  });

  it('creates a catalog item with the existing request structure', () => {
    component.openCreate();
    component.form = {
      serviceCode: '',
      serviceName: 'Mở tài khoản',
      serviceType: 'TÀI KHOẢN',
      description: 'Mở tài khoản mới',
      estimatedTime: 20,
      delegatable: false,
    };

    component.create();

    expect(api.createCatalogItem).toHaveBeenCalledWith({
      serviceCode: '',
      serviceName: 'Mở tài khoản',
      serviceType: 'TÀI KHOẢN',
      description: 'Mở tài khoản mới',
      estimatedTime: 20,
      delegatable: false,
    });
    expect(component.isEditorOpen).toBe(false);
  });

  it('keeps edit and restore workflows', () => {
    component.edit(active);
    component.form.serviceName = 'Phát hành thẻ mới';
    component.create();
    expect(api.updateCatalogItem).toHaveBeenCalledWith(
      1,
      expect.objectContaining({
        serviceName: 'Phát hành thẻ mới',
      }),
    );

    component.restore(archived);
    expect(api.restoreCatalogItem).toHaveBeenCalledWith(2);
  });

  it('requires the shared confirmation dialog before deleting', async () => {
    component.remove(active);
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    expect(api.deleteCatalogItem).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('app-confirm-dialog [role="dialog"]')).toBeTruthy();

    component.confirmRemove();
    expect(api.deleteCatalogItem).toHaveBeenCalledWith(1);
    expect(component.pendingDelete).toBeNull();
  });

  it('keeps search, status and type filtering', () => {
    component.searchTerm = 'cũ';
    expect(component.filteredCatalog).toEqual([archived]);
    component.searchTerm = '';
    component.statusFilter = 'ACTIVE';
    expect(component.filteredCatalog).toEqual([active]);
    component.statusFilter = 'ALL';
    component.typeFilter = 'THẺ';
    expect(component.filteredCatalog).toHaveLength(2);
  });
});
