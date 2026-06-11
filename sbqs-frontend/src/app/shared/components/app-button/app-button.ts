import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-button',
  imports: [],
  templateUrl: './app-button.html',
  styleUrl: './app-button.scss'
})
export class AppButton {

  label = input('');

  clicked = output<void>();

  onClick() {
    this.clicked.emit();
  }
}