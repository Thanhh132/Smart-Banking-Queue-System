import { Component, input } from '@angular/core';

import { AppIcon, AppIconName } from '../app-icon/app-icon';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [AppIcon],
  templateUrl: './app-empty-state.html',
  styleUrl: './app-empty-state.scss',
})
export class AppEmptyState {
  readonly title = input('Chưa có dữ liệu');
  readonly message = input('Không có dữ liệu phù hợp để hiển thị.');
  readonly icon = input<AppIconName>('list-checks');
}
