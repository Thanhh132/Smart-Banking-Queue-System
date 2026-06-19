import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-button',
  imports: [],
  templateUrl: './app-button.html',
  styleUrl: './app-button.scss'
})
export class AppButton {

  label = input('');
  variant = input<'primary' | 'secondary' | 'success' | 'danger'>('primary');
  disabled = input(false);

  clicked = output<void>();

  onClick() {
    if (this.disabled()) {
      return;
    }

    this.clicked.emit();
  }
}
