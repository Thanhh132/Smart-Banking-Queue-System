import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppStatusBadge } from './app-status-badge';

describe('AppStatusBadge', () => {
  let fixture: ComponentFixture<AppStatusBadge>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [AppStatusBadge] }).compileComponents();
    fixture = TestBed.createComponent(AppStatusBadge);
  });

  it('maps known statuses to semantic presentation', () => {
    fixture.componentRef.setInput('status', 'SERVING');
    fixture.detectChanges();

    const badge: HTMLElement = fixture.nativeElement.querySelector('.app-status-badge');
    expect(badge.textContent).toContain('Đang phục vụ');
    expect(badge.classList).toContain('app-status-badge--primary');
  });

  it('falls back to a neutral badge for unknown values', () => {
    fixture.componentRef.setInput('status', 'CUSTOM');
    fixture.detectChanges();

    const badge: HTMLElement = fixture.nativeElement.querySelector('.app-status-badge');
    expect(badge.textContent).toContain('CUSTOM');
    expect(badge.classList).toContain('app-status-badge--secondary');
  });
});
