import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppTopbar } from './app-topbar';

describe('AppTopbar', () => {
  let component: AppTopbar;
  let fixture: ComponentFixture<AppTopbar>;

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [AppTopbar],
    }).compileComponents();

    fixture = TestBed.createComponent(AppTopbar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it.each([
    ['SUPER_ADMIN', 'Quản trị hệ thống', '/super-admin'],
    ['BRANCH_ADMIN', 'Quản trị chi nhánh', '/admin'],
    ['STAFF', 'Nhân viên', '/staff'],
    ['CUSTOMER', 'Khách hàng', '/customer'],
  ])('shows the current %s role without adding fake account data', (role, label, homeRoute) => {
    sessionStorage.setItem('userRole', role);

    expect(component.roleLabel).toBe(label);
    expect(component.homeRoute).toBe(homeRoute);
  });

  it('emits a sidebar toggle request', () => {
    let toggleCount = 0;
    component.sidebarToggle.subscribe(() => toggleCount++);

    component.toggleSidebar();

    expect(toggleCount).toBe(1);
  });

  it('keeps the page title in the breadcrumb without rendering a duplicate h1', () => {
    fixture.componentRef.setInput('title', 'Quản lý nhân viên');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.topbar__breadcrumb').textContent).toContain(
      'Quản lý nhân viên',
    );
    expect(fixture.nativeElement.querySelector('h1')).toBeFalsy();
  });
});
