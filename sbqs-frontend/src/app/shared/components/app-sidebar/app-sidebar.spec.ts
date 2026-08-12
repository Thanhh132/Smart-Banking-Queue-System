import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppSidebar } from './app-sidebar';

describe('AppSidebar', () => {
  let component: AppSidebar;
  let fixture: ComponentFixture<AppSidebar>;

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [AppSidebar],
    }).compileComponents();

    fixture = TestBed.createComponent(AppSidebar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it.each([
    ['SUPER_ADMIN', ['/super-admin', '/super-admin/branches', '/super-admin/services', '/account']],
    [
      'BRANCH_ADMIN',
      [
        '/admin',
        '/admin/operations',
        '/admin/services',
        '/admin/mappings',
        '/admin/users',
        '/admin/history',
        '/monitor',
        '/account',
      ],
    ],
    ['STAFF', ['/staff', '/staff/history', '/monitor', '/account']],
    ['CUSTOMER', ['/customer', '/branches', '/ticket', '/delegations', '/account']],
  ])('keeps the expected navigation for %s', (role, expectedRoutes) => {
    sessionStorage.setItem('userRole', role);

    expect(component.menuItems.map((item) => item.route)).toEqual(expectedRoutes);
  });

  it('requests closing the mobile sidebar after navigation', () => {
    let closeCount = 0;
    component.closeRequested.subscribe(() => closeCount++);

    component.closeMobileSidebar();

    expect(closeCount).toBe(1);
  });

  it('requests desktop collapse from the sidebar footer', () => {
    let collapseCount = 0;
    component.collapseRequested.subscribe(() => collapseCount++);

    component.toggleCollapse();

    expect(collapseCount).toBe(1);
  });
});
