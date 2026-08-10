import { TestBed } from '@angular/core/testing';

import { AppConfirmDialog } from './app-confirm-dialog';

describe('AppConfirmDialog', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [AppConfirmDialog] }).compileComponents();
  });

  it('does not render until opened', () => {
    const fixture = TestBed.createComponent(AppConfirmDialog);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeFalsy();
  });

  it('emits confirmation from danger mode', () => {
    const fixture = TestBed.createComponent(AppConfirmDialog);
    const confirmed = vi.fn();
    fixture.componentInstance.confirmed.subscribe(confirmed);
    fixture.componentRef.setInput('open', true);
    fixture.componentRef.setInput('danger', true);
    fixture.detectChanges();

    const buttons = fixture.nativeElement.querySelectorAll('button');
    buttons[1].click();

    expect(buttons[1].classList).toContain('btn-danger');
    expect(confirmed).toHaveBeenCalledOnce();
  });

  it('emits cancellation when escape is pressed', () => {
    const fixture = TestBed.createComponent(AppConfirmDialog);
    const cancelled = vi.fn();
    fixture.componentInstance.cancelled.subscribe(cancelled);
    fixture.componentRef.setInput('open', true);
    fixture.detectChanges();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(cancelled).toHaveBeenCalledOnce();
  });
});
