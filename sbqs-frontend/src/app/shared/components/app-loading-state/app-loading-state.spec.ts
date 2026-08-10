import { TestBed } from '@angular/core/testing';

import { AppLoadingState } from './app-loading-state';

describe('AppLoadingState', () => {
  it('renders an accessible loading label', async () => {
    await TestBed.configureTestingModule({ imports: [AppLoadingState] }).compileComponents();
    const fixture = TestBed.createComponent(AppLoadingState);
    fixture.componentRef.setInput('label', 'Đang đồng bộ...');
    fixture.detectChanges();

    const state: HTMLElement = fixture.nativeElement.querySelector('[role="status"]');
    expect(state.textContent).toContain('Đang đồng bộ...');
  });
});
