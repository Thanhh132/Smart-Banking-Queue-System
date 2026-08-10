import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DashboardLayout } from './dashboard-layout';

describe('DashboardLayout', () => {
  let component: DashboardLayout;
  let fixture: ComponentFixture<DashboardLayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardLayout],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardLayout);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('collapses the fixed sidebar when toggled on desktop', () => {
    component.toggleSidebar();

    expect(component.sidebarCollapsed()).toBe(true);
    expect(component.sidebarOpen()).toBe(false);
  });

  it('opens and closes the drawer when toggled on mobile', () => {
    const originalMatchMedia = window.matchMedia;
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      value: () => ({ matches: true }) as MediaQueryList,
    });

    try {
      component.toggleSidebar();
      expect(component.sidebarOpen()).toBe(true);
      expect(component.sidebarCollapsed()).toBe(false);

      component.closeMobileSidebar();
      expect(component.sidebarOpen()).toBe(false);
    } finally {
      Object.defineProperty(window, 'matchMedia', {
        configurable: true,
        value: originalMatchMedia,
      });
    }
  });
});
