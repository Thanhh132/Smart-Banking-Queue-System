import { Component, computed, input, output } from '@angular/core';

import { AppIcon, AppIconName } from '../app-icon/app-icon';

export type AppButtonVariant =
  | 'primary'
  | 'secondary'
  | 'outline'
  | 'success'
  | 'warning'
  | 'danger';

@Component({
  selector: 'app-button',
  imports: [AppIcon],
  templateUrl: './app-button.html',
  styleUrl: './app-button.scss'
})
export class AppButton {
  readonly label = input('');
  readonly variant = input<AppButtonVariant>('primary');
  readonly size = input<'sm' | 'md'>('md');
  readonly type = input<'button' | 'submit' | 'reset'>('button');
  readonly icon = input<AppIconName | null>(null);
  readonly loading = input(false);
  readonly disabled = input(false);
  readonly outline = input(false);
  readonly ariaLabel = input('');

  readonly clicked = output<void>();

  readonly buttonClass = computed(() => {
    const variant = this.variant();
    const color = variant === 'outline' ? 'secondary' : variant;
    const prefix = this.outline() || variant === 'outline' ? 'btn-outline-' : 'btn-';
    return `${prefix}${color}`;
  });

  onClick(): void {
    if (this.disabled() || this.loading()) {
      return;
    }

    this.clicked.emit();
  }
}
