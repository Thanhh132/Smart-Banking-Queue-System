import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { AdminUsers } from './admin-users';

describe('AdminUsers', () => {
  let component: AdminUsers;
  let fixture: ComponentFixture<AdminUsers>;

  const staff = {
    userId: 12,
    fullName: 'Nguyễn Văn An',
    email: 'staff@sbqs.com',
    phone: '0901234567',
    role: 'STAFF',
    status: 'ACTIVE',
  };

  const userManagementService = {
    getUsersByRole: vi.fn(() => of([staff])),
    createStaff: vi.fn(() => of({})),
    updateUser: vi.fn(() => of({})),
    deleteUser: vi.fn(() => of({})),
  };

  beforeEach(async () => {
    sessionStorage.setItem('selectedBranchId', '8');
    vi.clearAllMocks();
    userManagementService.getUsersByRole.mockReturnValue(of([staff]));

    await TestBed.configureTestingModule({
      imports: [AdminUsers],
      providers: [
        { provide: UserManagementService, useValue: userManagementService },
        {
          provide: ApiErrorService,
          useValue: { getMessage: vi.fn((_error: unknown, fallback: string) => fallback) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUsers);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => {
    sessionStorage.removeItem('selectedBranchId');
  });

  it('loads and renders the staff list in the shared table shell', () => {
    expect(userManagementService.getUsersByRole).toHaveBeenCalledWith('STAFF');
    expect(component.users).toEqual([staff]);
    expect(fixture.nativeElement.querySelector('app-data-table-shell')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Nguyễn Văn An');
  });

  it('creates a valid staff account with the existing request structure', () => {
    component.openCreateModal();
    component.staffForm.setValue({
      fullName: 'Trần Thị Bình',
      email: 'binh@sbqs.com',
      phone: '0912345678',
      password: 'Strong1!',
      confirmPassword: 'Strong1!',
    });

    component.createStaff();

    expect(userManagementService.createStaff).toHaveBeenCalledWith({
      fullName: 'Trần Thị Bình',
      email: 'binh@sbqs.com',
      phone: '0912345678',
      password: 'Strong1!',
      confirmPassword: 'Strong1!',
      role: 'STAFF',
      branchId: 8,
    });
    expect(component.isUserModalOpen).toBe(false);
  });

  it('keeps invalid forms on the page and marks controls as touched', () => {
    component.openCreateModal();
    component.staffForm.reset();
    component.createStaff();

    expect(component.staffForm.get('fullName')?.touched).toBe(true);
    expect(userManagementService.createStaff).not.toHaveBeenCalled();
    expect(component.isUserModalOpen).toBe(true);
  });

  it('keeps the existing edit and update workflow', () => {
    component.startEdit(staff);
    expect(component.isUserModalOpen).toBe(true);
    component.staffForm.patchValue({ fullName: 'Nguyễn Văn An Mới' });
    component.createStaff();

    expect(userManagementService.updateUser).toHaveBeenCalledWith(12, {
      fullName: 'Nguyễn Văn An Mới',
      email: 'staff@sbqs.com',
      phone: '0901234567',
    });
  });

  it('requires the shared confirmation dialog before deleting', async () => {
    component.deleteUser(staff);
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();

    expect(userManagementService.deleteUser).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeTruthy();

    component.confirmDelete();

    expect(userManagementService.deleteUser).toHaveBeenCalledWith(12);
    expect(component.users).toEqual([]);
    expect(component.pendingDeleteUser).toBeNull();
  });

  it('renders user status through the shared status badge', () => {
    fixture.detectChanges();
    const badge: HTMLElement = fixture.nativeElement.querySelector(
      'app-status-badge [data-status]',
    );

    expect(badge.dataset['status']).toBe('ACTIVE');
  });

  it('renders compact loading and empty states through the table shell', async () => {
    component.isListLoading = true;
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('app-loading-state')).toBeTruthy();

    component.isListLoading = false;
    component.users = [];
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('app-empty-state')).toBeTruthy();
  });

  it('uses the responsive CRUD structure and shared action buttons', () => {
    expect(fixture.nativeElement.querySelector('.users-crud-layout')).toBeFalsy();
    expect(fixture.nativeElement.querySelector('app-modal-shell [role="dialog"]')).toBeFalsy();
    expect(fixture.nativeElement.querySelector('app-excel-import-panel')).toBeFalsy();
    expect(fixture.nativeElement.querySelectorAll('app-page-header app-button')).toHaveLength(2);
    expect(fixture.nativeElement.querySelectorAll('.user-row-actions app-button')).toHaveLength(2);
  });

  it('opens create and Excel import tools in separate responsive dialogs', async () => {
    component.openCreateModal();
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('app-modal-shell [role="dialog"]')).toBeTruthy();

    component.closeUserModal();
    component.openImportModal();
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('app-modal-shell [role="dialog"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('app-excel-import-panel')).toBeTruthy();
  });
});
