import { Component, HostListener, input, output } from '@angular/core';

import { AppButton } from '../app-button/app-button';

@Component({
  selector: 'app-modal-shell',
  standalone: true,
  imports: [AppButton],
  templateUrl: './app-modal-shell.html',
  styleUrl: './app-modal-shell.scss',
})
export class AppModalShell {
  readonly open = input(false);
  readonly title = input('');
  readonly description = input('');
  readonly size = input<'sm' | 'md' | 'lg' | 'xl'>('md');
  readonly closeDisabled = input(false);

  readonly closed = output<void>();

  close(): void {
    if (!this.closeDisabled()) {
      this.closed.emit();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open()) {
      this.close();
    }
  }
}
