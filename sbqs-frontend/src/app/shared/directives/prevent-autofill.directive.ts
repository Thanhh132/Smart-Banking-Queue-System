import { Directive, ElementRef, HostListener, inject } from '@angular/core';

/**
 * Prevents browsers and password managers from populating account-creation
 * fields before the user interacts with them.
 */
@Directive({
  selector: 'input[appPreventAutofill]',
  standalone: true,
})
export class PreventAutofillDirective {
  private readonly input = inject<ElementRef<HTMLInputElement>>(ElementRef).nativeElement;

  constructor() {
    this.input.readOnly = true;
    this.input.setAttribute('data-1p-ignore', 'true');
    this.input.setAttribute('data-lpignore', 'true');
    this.input.setAttribute('data-form-type', 'other');

    if (!this.input.hasAttribute('autocomplete')) {
      this.input.setAttribute('autocomplete', 'off');
    }
  }

  @HostListener('pointerdown')
  @HostListener('keydown')
  @HostListener('paste')
  @HostListener('drop')
  unlock(): void {
    this.input.readOnly = false;
  }
}
