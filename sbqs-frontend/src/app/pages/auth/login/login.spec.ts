import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService, DevLoginAccount, LoginResponse } from '../../../core/services/auth.service';
import { Login } from './login';

describe('Login quick accounts', () => {
  let fixture: ComponentFixture<Login>;
  let component: Login;
  const account: DevLoginAccount = {
    userId: 7,
    displayName: 'Test Staff',
    role: 'STAFF',
    branchName: 'Branch A',
  };
  const response: LoginResponse = {
    accessToken: 'dev.jwt',
    refreshToken: null,
    tokenType: 'Bearer',
    expiresIn: 300,
    role: 'STAFF',
    fullName: 'Test Staff',
    email: 'staff@example.com',
    branchId: 1,
    authenticationSource: 'DEV_QUICK_LOGIN',
    profileComplete: true,
  };
  const authService = {
    getDevLoginAccounts: vi.fn<() => Observable<DevLoginAccount[]>>(() => of([account])),
    devLogin: vi.fn<(userId: number) => Observable<LoginResponse>>(() => of(response)),
    getPostLoginRoute: vi.fn(() => '/staff'),
    login: vi.fn(),
    startGoogleLogin: vi.fn(),
    clearLocalSession: vi.fn(),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    authService.getDevLoginAccounts.mockReturnValue(of([account]));
    authService.devLogin.mockReturnValue(of(response));
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: ApiErrorService, useValue: {
          getMessage: vi.fn((_error: unknown, fallback: string) => fallback),
        } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture?.destroy();
  });

  it('shows Dev Login only after the backend confirms it is available', () => {
    fixture.detectChanges();
    expect(component.devLoginAvailable).toBe(true);
    expect(component.devAccounts).toEqual([account]);
    expect(authService.getDevLoginAccounts).toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('.dev-login-accounts')).not.toBeNull();
  });

  it('keeps the normal login UI clean when Dev Login is unavailable', () => {
    authService.getDevLoginAccounts.mockReturnValue(throwError(() => new Error('Not Found')));
    component.loadDevLoginAccounts();
    fixture.detectChanges();

    expect(component.devLoginAvailable).toBe(false);
    expect(component.devAccounts).toEqual([]);
    expect(fixture.nativeElement.querySelector('.dev-login-accounts')).toBeNull();
    expect(fixture.nativeElement.querySelector('.auth-form')).not.toBeNull();
  });

  it('logs in immediately when an active account is clicked', () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    component.loginDevAccount(account);

    expect(authService.devLogin).toHaveBeenCalledWith(7);
    expect(navigate).toHaveBeenCalledWith('/staff');
  });
});
