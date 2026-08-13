import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { AdminServicesService } from '../../../core/services/admin-services.service';
import { BranchService } from '../../../core/services/branch.service';
import { ReportService } from '../../../core/services/report.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { SuperAdmin } from './super-admin';

describe('SuperAdmin', () => {
  let fixture: ComponentFixture<SuperAdmin>;
  let component: SuperAdmin;
  const branches = [
    { branchId: 1, branchName: 'Chi nhánh 1', status: 'ACTIVE' },
    { branchId: 2, branchName: 'Chi nhánh 2', status: 'INACTIVE' },
  ];
  const branchAdmins = [{ userId: 9, role: 'BRANCH_ADMIN', branch: { branchId: 1 } }];
  const staff = [{ userId: 10, role: 'STAFF', branch: { branchId: 1 } }];
  const catalog = [{ catalogId: 1, status: 'ACTIVE' }, { catalogId: 2, status: 'ARCHIVED' }];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SuperAdmin],
      providers: [
        provideRouter([]),
        { provide: BranchService, useValue: { getBranches: vi.fn(() => of(branches)) } },
        { provide: UserManagementService, useValue: { getUsersByRole: vi.fn((role: string) => of(role === 'STAFF' ? staff : branchAdmins)) } },
        { provide: AdminServicesService, useValue: { getCatalog: vi.fn(() => of(catalog)) } },
        { provide: ReportService, useValue: { export: vi.fn(() => of(new Blob())), save: vi.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(SuperAdmin);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('shows operational statistics instead of the branch administrator list', () => {
    expect(component.activeBranches).toBe(1);
    expect(component.branchesWithoutAdmin).toBe(1);
    expect(component.activeCatalog).toBe(1);
    expect(fixture.nativeElement.querySelectorAll('.sbqs-metric')).toHaveLength(4);
    expect(fixture.nativeElement.querySelector('.dashboard-admin-table')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Trình tự quản trị');
  });

  it('navigates through the ordered management flow', () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    component.navigateToBranches();
    component.navigateToCatalog();
    expect(navigate).toHaveBeenNthCalledWith(1, ['/super-admin/branches']);
    expect(navigate).toHaveBeenNthCalledWith(2, ['/super-admin/services']);
  });
});
