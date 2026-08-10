import { TestBed } from '@angular/core/testing';

import { AppEmptyState } from './app-empty-state';

describe('AppEmptyState', () => {
  it('renders compact default content', async () => {
    await TestBed.configureTestingModule({ imports: [AppEmptyState] }).compileComponents();
    const fixture = TestBed.createComponent(AppEmptyState);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Chưa có dữ liệu');
    expect(fixture.nativeElement.querySelector('app-icon')).toBeTruthy();
  });
});
