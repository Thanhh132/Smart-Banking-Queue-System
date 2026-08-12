import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AdminServicesService } from '../../../core/services/admin-services.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { BranchService } from '../../../core/services/branch.service';
import { DelegationService } from '../../../core/services/delegation.service';
import { LocationService } from '../../../core/services/location.service';
import { ServicesService } from '../../../core/services/services.service';
import { CustomerDelegations } from './customer-delegations';

describe('CustomerDelegations', () => {
  const activeDelegation: any = { delegationId: 4, referenceCode: 'UQ-1234', status: 'ACTIVE', delegateName: 'Nguyễn Văn A', maskedIdentity: '********1234', serviceName: 'Nộp tiền', branchName: 'Chi nhánh 1', validUntil: new Date().toISOString() };
  const delegationApi = { getMine: vi.fn(() => of([activeDelegation])), create: vi.fn(() => of(activeDelegation)), cancel: vi.fn(() => of({ ...activeDelegation, status: 'CANCELLED' })) };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [CustomerDelegations],
      providers: [
        { provide: DelegationService, useValue: delegationApi },
        { provide: AdminServicesService, useValue: { getCatalog: () => of([]) } },
        { provide: BranchService, useValue: { getSmartRecommendations: () => of([]) } },
        { provide: ServicesService, useValue: { getMappedServicesByBranch: () => of([]) } },
        { provide: LocationService, useValue: { googleMapsUrl: () => '' } },
        { provide: ApiErrorService, useValue: { getMessage: (_: unknown, fallback: string) => fallback } },
      ],
    }).compileComponents();
  });

  it('renders the guided flow and requires shared confirmation before cancellation', async () => {
    const fixture = TestBed.createComponent(CustomerDelegations);
    const component = fixture.componentInstance;
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.sbqs-journey')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('UQ-1234');
    component.cancel(activeDelegation);
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    expect(delegationApi.cancel).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('app-confirm-dialog [role="dialog"]')).toBeTruthy();

    component.confirmCancellation();
    expect(delegationApi.cancel).toHaveBeenCalledWith(4);
  });
});
