import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppModalShell } from './app-modal-shell';

describe('AppModalShell', () => {
  let fixture: ComponentFixture<AppModalShell>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [AppModalShell] }).compileComponents();
    fixture = TestBed.createComponent(AppModalShell);
  });

  it('renders an accessible dialog and emits close', () => {
    fixture.componentRef.setInput('open', true);
    fixture.componentRef.setInput('title', 'Cập nhật dữ liệu');
    fixture.componentRef.setInput('description', 'Thông tin mô tả');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Cập nhật dữ liệu');

    const closed = vi.fn();
    fixture.componentInstance.closed.subscribe(closed);
    fixture.componentInstance.close();
    expect(closed).toHaveBeenCalledOnce();
  });

  it('does not close while disabled', () => {
    fixture.componentRef.setInput('open', true);
    fixture.componentRef.setInput('closeDisabled', true);
    const closed = vi.fn();
    fixture.componentInstance.closed.subscribe(closed);

    fixture.componentInstance.close();
    expect(closed).not.toHaveBeenCalled();
  });
});
