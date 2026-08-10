import { Component, HostListener, input, output } from '@angular/core';

import { AppButton } from '../app-button/app-button';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [AppButton],
  templateUrl: './app-confirm-dialog.html',
  styleUrl: './app-confirm-dialog.scss',
})
export class AppConfirmDialog {
  readonly open = input(false);
  readonly title = input('Xác nhận thao tác');
  readonly message = input('Bạn có chắc chắn muốn tiếp tục?');
  readonly confirmLabel = input('Xác nhận');
  readonly cancelLabel = input('Hủy');
  readonly danger = input(false);
  readonly loading = input(false);

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  confirm(): void {
    if (!this.loading()) {
      this.confirmed.emit();
    }
  }

  cancel(): void {
    if (!this.loading()) {
      this.cancelled.emit();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open()) {
      this.cancel();
    }
  }
}
