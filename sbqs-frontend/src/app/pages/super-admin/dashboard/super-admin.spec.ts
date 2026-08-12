import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { BranchService } from '../../../core/services/branch.service';
import { ReportService } from '../../../core/services/report.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { SuperAdmin } from './super-admin';

describe('SuperAdmin', () => {
  let fixture: ComponentFixture<SuperAdmin>;
  let component: SuperAdmin;

  const branch = { branchId: 4, branchName: 'BIDV Quận 1', branchCode: 'BIDV-Q1-01' };
  const admin = {
    userId: 9,
    fullName: 'Nguyễn Quản Trị',
    email: 'admin@sbqs.vn',
    phone: '0901234567',
    branch,
  };
  const branchService = { getBranches: vi.fn(() => of([branch])) };
  const userService = {
    getUsersByRole: vi.fn(() => of([admin])),
    createAdminBranch: vi.fn(() => of({})),
    updateUser: vi.fn(() => of({})),
    deleteUser: vi.fn(() => of(void 0)),
  };
  let router: Router;
  const reportService = { export: vi.fn(() => of(new Blob())), save: vi.fn() };

  beforeEach(async () => {
    vi.clearAllMocks();
    branchService.getBranches.mockReturnValue(of([branch]));
    userService.getUsersByRole.mockReturnValue(of([admin]));

    await TestBed.configureTestingModule({
      imports: [SuperAdmin],
      providers: [
        provideRouter([]),
        { provide: BranchService, useValue: branchService },
        { provide: UserManagementService, useValue: userService },
        { provide: ReportService, useValue: reportService },
        {
          provide: ApiErrorService,
          useValue: { getMessage: vi.fn((_error: unknown, fallback: string) => fallback) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SuperAdmin);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    await fixture.whenStable();
  });

  it('renders dashboard metrics, reports and the shared administrator table', () => {
    expect(branchService.getBranches).toHaveBeenCalled();
    expect(userService.getUsersByRole).toHaveBeenCalledWith('BRANCH_ADMIN');
    expect(fixture.nativeElement.querySelectorAll('.sbqs-metric')).toHaveLength(3);
    expect(fixture.nativeElement.querySelectorAll('app-report-export-buttons')).toHaveLength(4);
    expect(fixture.nativeElement.querySelector('app-data-table-shell')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Nguyễn Quản Trị');
  });

  it('creates an administrator with the existing payload', () => {
    component.openCreateAdminBranch();
    component.adminBranchForm.setValue({
      fullName: 'Trần Quản Trị',
      email: 'tran@sbqs.vn',
      phone: '0912345678',
      password: 'Strong1!',
      confirmPassword: 'Strong1!',
      branchId: 4,
    });
    component.submitAdminBranch();

    expect(userService.createAdminBranch).toHaveBeenCalledWith({
      fullName: 'Trần Quản Trị',
      email: 'tran@sbqs.vn',
      phone: '0912345678',
      password: 'Strong1!',
      confirmPassword: 'Strong1!',
      branchId: 4,
    });
    expect(component.isAdminModalOpen).toBe(false);
  });

  it('keeps the existing edit payload', () => {
    component.startEditAdminBranch(admin);
    component.adminBranchForm.patchValue({ fullName: 'Nguyễn Quản Trị Mới' });
    component.submitAdminBranch();

    expect(userService.updateUser).toHaveBeenCalledWith(9, {
      fullName: 'Nguyễn Quản Trị Mới',
      email: 'admin@sbqs.vn',
      phone: '0901234567',
      branchId: 4,
    });
  });

  it('requires shared confirmation before deleting an administrator', async () => {
    component.deleteAdminBranch(admin);
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    expect(userService.deleteUser).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('app-confirm-dialog [role="dialog"]')).toBeTruthy();

    component.confirmDeleteAdminBranch();
    expect(userService.deleteUser).toHaveBeenCalledWith(9);
    expect(component.pendingDeleteAdmin).toBeNull();
  });

  it('keeps search and navigation behavior', () => {
    component.searchTerm = 'BIDV Quận 1';
    expect(component.filteredAdmins).toEqual([admin]);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    component.navigateToBranches();
    expect(navigate).toHaveBeenCalledWith(['/super-admin/branches']);
  });
});
