import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppButton } from './app-button';

describe('AppButton', () => {
  let component: AppButton;
  let fixture: ComponentFixture<AppButton>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppButton],
    }).compileComponents();

    fixture = TestBed.createComponent(AppButton);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('maps variant, size and icon to the CoreUI button', () => {
    fixture.componentRef.setInput('variant', 'warning');
    fixture.componentRef.setInput('size', 'sm');
    fixture.componentRef.setInput('icon', 'save');
    fixture.componentRef.setInput('label', 'Lưu');
    fixture.detectChanges();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(button.classList).toContain('btn-warning');
    expect(button.classList).toContain('btn-sm');
    expect(fixture.nativeElement.querySelector('app-icon')).toBeTruthy();
  });

  it('disables interaction while loading', () => {
    const clicked = vi.fn();
    component.clicked.subscribe(clicked);
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    button.click();

    expect(button.disabled).toBe(true);
    expect(button.getAttribute('aria-busy')).toBe('true');
    expect(clicked).not.toHaveBeenCalled();
  });
});
