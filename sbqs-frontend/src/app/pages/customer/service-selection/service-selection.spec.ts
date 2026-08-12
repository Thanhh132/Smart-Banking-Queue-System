import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServiceSelection } from './service-selection';

describe('ServiceSelection', () => {
  let component: ServiceSelection;
  let fixture: ComponentFixture<ServiceSelection>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServiceSelection],
    }).compileComponents();

    fixture = TestBed.createComponent(ServiceSelection);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('keeps the dynamic schema and required validation when selecting a service', () => {
    component.selectService({
      serviceId: 4,
      serviceCode: 'DEPOSIT',
      serviceName: 'Nộp tiền',
      serviceType: 'TRANSACTION',
      estimatedTime: 10,
      status: 'ACTIVE',
      formSchema: [
        {
          key: 'accountNumber',
          label: 'Số tài khoản',
          type: 'TEXT',
          required: true,
          placeholder: '',
          section: 'Thông tin giao dịch',
          options: [],
        },
      ],
    });

    expect(component.selectedService?.serviceCode).toBe('DEPOSIT');
    expect(component.transactionForm.get('accountNumber')?.invalid).toBe(true);
    component.transactionForm.get('accountNumber')?.setValue('0123456789');
    expect(component.transactionForm.valid).toBe(true);
  });

  it('does not auto-fill transaction-specific account or delivery fields from legacy profile data', () => {
    (component as any).profileValues = {
      ACCOUNT_NUMBER: 'legacy-account',
      CARD_DELIVERY_ADDRESS: 'legacy-delivery-address',
      CONTACT_ADDRESS: 'profile-contact-address',
    };

    component.selectService({
      serviceId: 5,
      serviceCode: 'CARD',
      serviceName: 'Làm thẻ',
      serviceType: 'TRANSACTION',
      estimatedTime: 10,
      status: 'ACTIVE',
      formSchema: [
        { key: 'accountNumber', label: 'Số tài khoản', type: 'TEXT', required: true, placeholder: '', section: 'Giao dịch', options: [] },
        { key: 'deliveryAddress', label: 'Địa chỉ giao thẻ', type: 'TEXT', required: true, placeholder: '', section: 'Giao dịch', options: [] },
      ],
    });

    expect(component.transactionForm.get('accountNumber')?.value).toBe('');
    expect(component.transactionForm.get('deliveryAddress')?.value).toBe('');
  });
});
