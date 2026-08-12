import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { Branch } from '../../../core/models/branch.model';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { BranchService } from '../../../core/services/branch.service';
import { LocationService } from '../../../core/services/location.service';
import { SuperAdminBranches } from './super-admin-branches';

describe('SuperAdminBranches', () => {
  let fixture: ComponentFixture<SuperAdminBranches>;
  let component: SuperAdminBranches;

  const branch: Branch = {
    branchId: 4,
    bankName: 'BIDV',
    branchCode: 'BIDV-Q1-01',
    branchName: 'BIDV Quận 1',
    province: 'Thành phố Hồ Chí Minh',
    district: 'Quận 1',
    ward: 'Phường Bến Nghé',
    address: '12 Nguyễn Huệ',
    phone: '0901234567',
    status: 'ACTIVE',
    latitude: 10.7769,
    longitude: 106.7009,
  };
  const branchService = {
    getBranches: vi.fn(() => of([branch])),
    createBranch: vi.fn((payload: object) => of({ ...branch, ...payload })),
    updateBranch: vi.fn((id: number, payload: object) =>
      of({ ...branch, ...payload, branchId: id }),
    ),
    deleteBranch: vi.fn(() => of(void 0)),
  };
  const locationService = {
    googleMapsUrl: vi.fn(() => 'https://maps.example/branch'),
    geocode: vi.fn(() =>
      of({
        formattedAddress: branch.address,
        latitude: branch.latitude,
        longitude: branch.longitude,
        province: branch.province,
        district: branch.district,
        ward: branch.ward,
      }),
    ),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    branchService.getBranches.mockReturnValue(of([branch]));

    await TestBed.configureTestingModule({
      imports: [SuperAdminBranches],
      providers: [
        { provide: BranchService, useValue: branchService },
        { provide: LocationService, useValue: locationService },
        {
          provide: ApiErrorService,
          useValue: { getMessage: vi.fn((_error: unknown, fallback: string) => fallback) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SuperAdminBranches);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('renders real metrics and branch data through shared table primitives', () => {
    expect(branchService.getBranches).toHaveBeenCalled();
    expect(fixture.nativeElement.querySelectorAll('.sbqs-metric')).toHaveLength(3);
    expect(fixture.nativeElement.querySelector('app-data-table-shell')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('app-status-badge')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('BIDV Quận 1');
  });

  it('opens the complex branch form in the shared large modal', async () => {
    component.openCreate();
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    expect(component.isEditorOpen).toBe(true);
    expect(fixture.nativeElement.querySelector('app-modal-shell [role="dialog"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.branch-map iframe')).toBeTruthy();
  });

  it('keeps the existing update payload and map coordinates', () => {
    component.startEdit(branch);
    component.branchForm.patchValue({ branchName: 'BIDV Quận 1 Mới' });
    component.submitBranch();

    expect(branchService.updateBranch).toHaveBeenCalledWith(
      4,
      expect.objectContaining({
        branchName: 'BIDV Quận 1 Mới',
        latitude: 10.7769,
        longitude: 106.7009,
      }),
    );
    expect(component.isEditorOpen).toBe(false);
  });

  it('keeps geocoding before creating an unconfirmed branch', () => {
    component.openCreate();
    component.branchForm.patchValue({
      address: branch.address,
      phone: branch.phone,
      status: 'ACTIVE',
    });
    component.submitBranch();

    expect(locationService.geocode).toHaveBeenCalled();
    expect(branchService.createBranch).toHaveBeenCalledWith(
      expect.objectContaining({
        latitude: 10.7769,
        longitude: 106.7009,
      }),
    );
  });

  it('requires shared confirmation before deleting a branch', async () => {
    component.deleteBranch(branch);
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    expect(branchService.deleteBranch).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('app-confirm-dialog [role="dialog"]')).toBeTruthy();

    component.confirmDeleteBranch();
    expect(branchService.deleteBranch).toHaveBeenCalledWith(4);
    expect(component.pendingDeleteBranch).toBeNull();
  });

  it('keeps branch search behavior', () => {
    component.searchTerm = 'Bến Nghé';
    expect(component.filteredBranches).toEqual([branch]);
    component.searchTerm = 'không tồn tại';
    expect(component.filteredBranches).toEqual([]);
  });
});
