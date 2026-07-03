import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { PreventAutofillDirective } from './prevent-autofill.directive';

@Component({
  imports: [PreventAutofillDirective],
  template: '<input appPreventAutofill type="email" />',
})
class TestHost {}

describe('PreventAutofillDirective', () => {
  function renderInput(): HTMLInputElement {
    const fixture = TestBed.createComponent(TestHost);
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('input');
  }

  it('locks and marks the input against automatic filling', () => {
    const input = renderInput();

    expect(input.readOnly).toBe(true);
    expect(input.autocomplete).toBe('off');
    expect(input.getAttribute('data-1p-ignore')).toBe('true');
    expect(input.getAttribute('data-lpignore')).toBe('true');
  });

  it('unlocks after genuine user interaction', () => {
    const input = renderInput();

    input.dispatchEvent(new Event('pointerdown'));

    expect(input.readOnly).toBe(false);
  });
});
