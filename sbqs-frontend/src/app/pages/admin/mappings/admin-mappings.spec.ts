import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { AdminMappingsService } from '../../../core/services/admin-mappings.service';
import { AdminMappings } from './admin-mappings';

describe('AdminMappings', () => {
  let component: AdminMappings;
  let fixture: ComponentFixture<AdminMappings>;

  const machine = {
    queueMachineId: 3,
    machineName: 'Máy sảnh chính',
    machineCode: 'M01',
    branch: { branchId: 8 },
  };
  const services = [
    { serviceId: 11, serviceName: 'Nộp tiền', serviceCode: 'NOP_TIEN' },
    { serviceId: 12, serviceName: 'Rút tiền', serviceCode: 'RUT_TIEN' },
  ];
  const mapping = {
    queueMachine: machine,
    service: services[0],
  };

  const mappingService = {
    getQueueMachines: vi.fn(() => of([machine])),
    getServices: vi.fn(() => of(services)),
    getMappings: vi.fn(() => of([mapping])),
    createMapping: vi.fn(() => of({})),
    deleteMapping: vi.fn(() => of('')),
  };

  beforeEach(async () => {
    sessionStorage.setItem('selectedBranchId', '8');
    vi.clearAllMocks();
    mappingService.getQueueMachines.mockReturnValue(of([machine]));
    mappingService.getServices.mockReturnValue(of(services));
    mappingService.getMappings.mockReturnValue(of([mapping]));
    mappingService.createMapping.mockReturnValue(of({}));
    mappingService.deleteMapping.mockReturnValue(of(''));

    await TestBed.configureTestingModule({
      imports: [AdminMappings],
      providers: [
        { provide: AdminMappingsService, useValue: mappingService },
        {
          provide: ApiErrorService,
          useValue: { getMessage: vi.fn((_error: unknown, fallback: string) => fallback) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminMappings);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => {
    sessionStorage.removeItem('selectedBranchId');
  });

  it('loads branch-scoped machines, services and mappings', () => {
    expect(mappingService.getQueueMachines).toHaveBeenCalledOnce();
    expect(mappingService.getServices).toHaveBeenCalledWith(8);
    expect(mappingService.getMappings).toHaveBeenCalledOnce();
    expect(component.queueMachines).toEqual([machine]);
    expect(component.services).toEqual(services);
    expect(component.mappings).toEqual([mapping]);
    expect(component.selectedQueueMachineId).toBe(3);
  });

  it('renders the context editor and full-width shared table shell', () => {
    expect(fixture.nativeElement.querySelector('.mapping-context')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.mapping-context__summary').textContent).toContain(
      '1 máy bốc số',
    );
    expect(fixture.nativeElement.querySelector('.mapping-context__summary').textContent).toContain(
      '2 dịch vụ',
    );
    expect(fixture.nativeElement.querySelector('.mapping-context__summary').textContent).toContain(
      '1 liên kết',
    );
    expect(fixture.nativeElement.querySelector('.mapping-service-grid')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('app-data-table-shell')).toBeTruthy();
    expect(fixture.nativeElement.querySelectorAll('.mapping-table tbody tr')).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('.mapping-page-grid')).toBeFalsy();
    expect(fixture.nativeElement.querySelector('.mapping-summary-grid')).toBeFalsy();
  });

  it('keeps the existing multi-service selection and create payloads', () => {
    component.toggleService(11, true);
    component.toggleService(12, true);
    component.createMappings();

    expect(mappingService.createMapping).toHaveBeenNthCalledWith(1, 3, 11);
    expect(mappingService.createMapping).toHaveBeenNthCalledWith(2, 3, 12);
    expect(component.selectedServiceIds).toEqual([]);
    expect(component.successMessage).toBe('Đã tạo 2 liên kết.');
  });

  it('requires a machine and at least one service before creating', () => {
    component.selectedQueueMachineId = null;
    component.selectedServiceIds = [];
    component.createMappings();

    expect(mappingService.createMapping).not.toHaveBeenCalled();
    expect(component.errorMessage).toContain('ít nhất một dịch vụ');
  });

  it('uses the shared confirmation dialog before deleting a mapping', async () => {
    component.deleteMapping(mapping);
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();

    expect(mappingService.deleteMapping).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeTruthy();

    component.confirmDeleteMapping();

    expect(mappingService.deleteMapping).toHaveBeenCalledWith(3, 11);
    expect(component.pendingDeleteMapping).toBeNull();
    expect(component.successMessage).toBe('Đã gỡ liên kết.');
  });

  it('renders shared loading and empty states', async () => {
    const pendingMappings = new Subject<any[]>();
    mappingService.getMappings.mockReturnValue(pendingMappings);
    const pendingFixture = TestBed.createComponent(AdminMappings);
    pendingFixture.detectChanges();

    expect(pendingFixture.nativeElement.querySelector('app-loading-state')).toBeTruthy();

    pendingMappings.next([]);
    pendingMappings.complete();
    await pendingFixture.whenStable();
    expect(pendingFixture.nativeElement.querySelector('app-empty-state')).toBeTruthy();
  });
});
