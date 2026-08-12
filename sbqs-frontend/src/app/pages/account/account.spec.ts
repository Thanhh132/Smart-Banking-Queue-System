import { ComponentFixture, TestBed } from '@angular/core/testing';
import { from, Observable, Subject, of } from 'rxjs';

import { AccountProfile, AccountService } from '../../core/services/account.service';
import { ApiErrorService } from '../../core/services/api-error.service';
import { Account } from './account';

describe('Account', () => {
  let fixture: ComponentFixture<Account> | undefined;
  let component: Account;

  const accountService = {
    getProfile: vi.fn<() => Observable<AccountProfile>>(),
    getPaperlessProfile: vi.fn(() =>
      of({
        values: {
          PERMANENT_ADDRESS: '12 Nguyễn Huệ',
          CONTACT_ADDRESS: '25 Lê Lợi',
        },
        requiredFields: [],
        missingFields: [],
        complete: true,
      }),
    ),
    updatePaperlessProfile: vi.fn(() => of({})),
    requestProfileChange: vi.fn(() => of(void 0)),
    changePassword: vi.fn(() => of(void 0)),
  };

  const roleNames: Record<string, string> = {
    SUPER_ADMIN: 'Quản trị hệ thống',
    BRANCH_ADMIN: 'Quản trị chi nhánh',
    STAFF: 'Nhân viên',
    CUSTOMER: 'Khách hàng',
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    accountService.getPaperlessProfile.mockReturnValue(
      from(
        Promise.resolve({
          values: {
            PERMANENT_ADDRESS: '12 Nguyễn Huệ',
            CONTACT_ADDRESS: '25 Lê Lợi',
          },
          requiredFields: [],
          missingFields: [],
          complete: true,
        }),
      ),
    );
    accountService.updatePaperlessProfile.mockReturnValue(of({}));
    accountService.requestProfileChange.mockReturnValue(of(void 0));
    accountService.changePassword.mockReturnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [Account],
      providers: [
        { provide: AccountService, useValue: accountService },
        {
          provide: ApiErrorService,
          useValue: { getMessage: vi.fn((_error: unknown, fallback: string) => fallback) },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    fixture?.destroy();
    fixture = undefined;
    sessionStorage.removeItem('userRole');
  });

  function profileFor(
    role: 'SUPER_ADMIN' | 'BRANCH_ADMIN' | 'STAFF' | 'CUSTOMER',
    overrides: Partial<AccountProfile> = {},
  ): AccountProfile {
    return {
      userId: 8,
      fullName: 'Nguyễn Văn An',
      email: 'an@sbqs.vn',
      phone: '0901234567',
      role,
      status: 'ACTIVE',
      branchId: role === 'SUPER_ADMIN' || role === 'CUSTOMER' ? null : 3,
      branchName: role === 'SUPER_ADMIN' || role === 'CUSTOMER' ? null : 'Chi nhánh Trung tâm',
      createdAt: '2026-08-11T08:00:00Z',
      profileComplete: true,
      passwordChangeAvailable: true,
      ...overrides,
    };
  }

  async function createComponent(profile: AccountProfile): Promise<void> {
    sessionStorage.setItem('userRole', profile.role);
    accountService.getProfile.mockReturnValue(from(Promise.resolve(profile)));
    fixture = TestBed.createComponent(Account);
    component = fixture.componentInstance;
    await fixture.whenStable();
  }

  it.each(['SUPER_ADMIN', 'BRANCH_ADMIN', 'STAFF', 'CUSTOMER'] as const)(
    'renders the shared account settings page correctly for %s',
    async (role) => {
      await createComponent(profileFor(role));

      const text = fixture!.nativeElement.textContent;
      expect(text).toContain('Tài khoản của tôi');
      expect(text).toContain(roleNames[role]);
      expect(text).toContain('Hoạt động');
      expect(text).toContain('Thông tin cá nhân và liên hệ');
      expect(text).toContain('Bảo mật và mật khẩu');
      expect(text).not.toContain('Vui lòng nhập mật khẩu hiện tại.');
      expect(fixture!.nativeElement.querySelector('.account-summary')).toBeTruthy();
      expect(fixture!.nativeElement.querySelector('.account-sections')).toBeTruthy();
      expect(fixture!.nativeElement.querySelector('.account-information-card')).toBeTruthy();
      expect(fixture!.nativeElement.querySelector('.account-security-card')).toBeTruthy();
      expect(fixture!.nativeElement.querySelectorAll('app-card')).toHaveLength(3);
      expect(fixture!.nativeElement.querySelector('.account-readonly-grid')).toBeTruthy();

      if (role === 'BRANCH_ADMIN' || role === 'STAFF') {
        expect(text).toContain('Chi nhánh Trung tâm');
      }

      if (role === 'CUSTOMER') {
        expect(text).toContain('Địa chỉ hồ sơ giao dịch');
        expect(fixture!.nativeElement.querySelector('[card-actions]')).toBeTruthy();
        expect(accountService.getPaperlessProfile).toHaveBeenCalledOnce();
      } else {
        expect(text).not.toContain('Địa chỉ hồ sơ giao dịch');
        expect(fixture!.nativeElement.querySelector('[card-actions]')).toBeFalsy();
        expect(accountService.getPaperlessProfile).not.toHaveBeenCalled();
      }
    },
  );

  it('loads and saves customer-only paperless addresses with the existing keys', async () => {
    await createComponent(profileFor('CUSTOMER'));
    expect(component.addressForm.getRawValue()).toEqual({
      permanentAddress: '12 Nguyễn Huệ',
      contactAddress: '25 Lê Lợi',
    });

    component.addressForm.setValue({
      permanentAddress: '30 Trần Hưng Đạo',
      contactAddress: '15 Pasteur',
    });
    component.saveAddresses();

    expect(accountService.updatePaperlessProfile).toHaveBeenCalledWith({
      values: {
        PERMANENT_ADDRESS: '30 Trần Hưng Đạo',
        CONTACT_ADDRESS: '15 Pasteur',
      },
    });
    expect(component.addressMessage).toContain('Đã lưu địa chỉ');
  });

  it('keeps the customer profile-change confirmation workflow', async () => {
    await createComponent(profileFor('CUSTOMER'));
    component.startEditProfile();
    component.profileForm.setValue({
      fullName: 'Nguyễn Văn An Mới',
      email: 'an.moi@sbqs.vn',
      phone: '0912345678',
    });
    component.saveProfile();

    expect(accountService.requestProfileChange).toHaveBeenCalledWith({
      fullName: 'Nguyễn Văn An Mới',
      email: 'an.moi@sbqs.vn',
      phone: '0912345678',
    });
    expect(component.isEditingProfile).toBe(false);
    expect(component.profileForm.disabled).toBe(true);
    expect(component.profileMessage).toContain('email hiện tại');
  });

  it('does not allow internal roles to enter the customer profile-change flow', async () => {
    await createComponent(profileFor('STAFF'));
    component.startEditProfile();
    component.saveProfile();

    expect(component.isEditingProfile).toBe(false);
    expect(accountService.requestProfileChange).not.toHaveBeenCalled();
  });

  it('keeps profile and address validation before submitting', async () => {
    await createComponent(profileFor('CUSTOMER'));
    component.startEditProfile();
    component.profileForm.setValue({ fullName: '', email: 'invalid', phone: '' });
    component.saveProfile();
    expect(component.profileForm.controls.fullName.touched).toBe(true);
    expect(accountService.requestProfileChange).not.toHaveBeenCalled();

    component.addressForm.setValue({ permanentAddress: '', contactAddress: '' });
    component.saveAddresses();
    expect(component.addressForm.controls.permanentAddress.touched).toBe(true);
    expect(accountService.updatePaperlessProfile).not.toHaveBeenCalled();
  });

  it('keeps the password payload, validation and reset behavior', async () => {
    await createComponent(profileFor('BRANCH_ADMIN'));
    component.passwordForm.setValue({
      currentPassword: 'Current1!',
      newPassword: 'NewStrong1!',
      confirmPassword: 'NewStrong1!',
    });
    component.changePassword();

    expect(accountService.changePassword).toHaveBeenCalledWith({
      currentPassword: 'Current1!',
      newPassword: 'NewStrong1!',
    });
    expect(component.passwordForm.getRawValue()).toEqual({
      currentPassword: null,
      newPassword: null,
      confirmPassword: null,
    });
    expect(component.passwordMessage).toBe('Mật khẩu đã được thay đổi.');
  });

  it('rejects a mismatched password confirmation without calling the API', async () => {
    await createComponent(profileFor('CUSTOMER'));
    component.passwordForm.setValue({
      currentPassword: 'Current1!',
      newPassword: 'NewStrong1!',
      confirmPassword: 'Different1!',
    });
    component.changePassword();

    expect(accountService.changePassword).not.toHaveBeenCalled();
    expect(component.passwordError).toBe('Mật khẩu xác nhận không khớp.');
  });

  it('shows required password validation only after submit is attempted', async () => {
    await createComponent(profileFor('STAFF'));
    expect(fixture!.nativeElement.textContent).not.toContain('Vui lòng nhập mật khẩu hiện tại.');

    component.changePassword();
    fixture!.detectChanges();

    expect(component.passwordForm.controls.currentPassword.touched).toBe(true);
    expect(fixture!.nativeElement.textContent).toContain('Vui lòng nhập mật khẩu hiện tại.');
    expect(accountService.changePassword).not.toHaveBeenCalled();
  });

  it('hides the password section when the backend marks it unavailable', async () => {
    await createComponent(profileFor('SUPER_ADMIN', { passwordChangeAvailable: false }));
    expect(fixture!.nativeElement.textContent).not.toContain('Bảo mật và mật khẩu');
  });

  it('uses the shared loading state and preserves the existing load error', async () => {
    const response = new Subject<AccountProfile>();
    accountService.getProfile.mockReturnValue(response);
    fixture = TestBed.createComponent(Account);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-loading-state')).toBeTruthy();

    response.error(new Error('offline'));
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    expect(component.loadError).toBe('Không tải được thông tin tài khoản.');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy();
  });
});
