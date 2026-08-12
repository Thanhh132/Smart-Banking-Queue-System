import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { Service } from '../../../core/models/service.model';
import { AdminServicesService } from '../../../core/services/admin-services.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AdminServices } from './admin-services';

describe('AdminServices', () => {
  let component: AdminServices;
  let fixture: ComponentFixture<AdminServices>;

  const service: Service = {
    serviceId: 21,
    serviceCode: 'TRANSFER',
    serviceName: 'Chuyển tiền',
    serviceType: 'TRANSACTION',
    estimatedTime: 12,
    status: 'ACTIVE',
    requiredCustomerFields: ['EXISTING_FIELD'],
    formSchema: [
      {
        key: 'account_number',
        label: 'Số tài khoản nhận',
        type: 'TEXT',
        required: true,
        placeholder: 'Nhập số tài khoản',
        section: 'Thông tin giao dịch',
        options: [],
      },
    ],
  };

  const adminServicesService = {
    getAllServices: vi.fn(),
    updateService: vi.fn(),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    adminServicesService.getAllServices.mockReturnValue(
      of([{ ...service, formSchema: service.formSchema?.map((field) => ({ ...field })) }]),
    );
    adminServicesService.updateService.mockReturnValue(of(service));

    await TestBed.configureTestingModule({
      imports: [AdminServices],
      providers: [
        { provide: AdminServicesService, useValue: adminServicesService },
        {
          provide: ApiErrorService,
          useValue: { getMessage: vi.fn((_error: unknown, fallback: string) => fallback) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminServices);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the selected service into the compact two-surface workspace', () => {
    expect(adminServicesService.getAllServices).toHaveBeenCalled();
    expect(component.selectedService?.serviceId).toBe(21);
    expect(fixture.nativeElement.querySelector('app-page-header')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.service-toolbar select')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.editor-workspace')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.schema-editor')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.preview-panel')).toBeTruthy();
    expect(fixture.nativeElement.querySelectorAll('app-card')).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('.service-picker-card')).toBeFalsy();
    expect(fixture.nativeElement.querySelector('.services-workflow')).toBeFalsy();
    expect(fixture.nativeElement.textContent).toContain('Chuyển tiền');
  });

  it('renders service status through the shared status badge', () => {
    const statuses: HTMLElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('app-status-badge [data-status]'),
    );

    expect(statuses.some((badge) => badge.dataset['status'] === 'ACTIVE')).toBe(true);
  });

  it('keeps the existing add, move and remove field behavior', () => {
    component.addField();
    expect(component.draftFields).toHaveLength(2);

    const addedKey = component.draftFields[1].key;
    component.move(1, -1);
    expect(component.draftFields[0].key).toBe(addedKey);

    component.removeField(0);
    expect(component.draftFields).toHaveLength(1);
    expect(component.draftFields[0].key).toBe('account_number');
  });

  it('saves the schema with the existing request structure', () => {
    component.save();

    expect(adminServicesService.updateService).toHaveBeenCalledWith(
      21,
      expect.objectContaining({
        serviceId: 21,
        formSchema: component.draftFields,
        requiredCustomerFields: [
          'FULL_NAME',
          'MOBILE_PHONE',
          'PERMANENT_ADDRESS',
          'CONTACT_ADDRESS',
          'EXISTING_FIELD',
        ],
      }),
    );
  });

  it('keeps duplicate-key validation and does not submit invalid schemas', () => {
    component.draftFields.push({ ...component.draftFields[0] });
    component.save();

    expect(component.errorMessage).toContain('không được trùng nhau');
    expect(adminServicesService.updateService).not.toHaveBeenCalled();
  });

  it('uses shared loading and empty states', async () => {
    component.isLoading = true;
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('app-loading-state')).toBeTruthy();

    component.isLoading = false;
    component.services = [];
    component.selectedService = null;
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('app-empty-state')).toHaveLength(1);
  });
});
