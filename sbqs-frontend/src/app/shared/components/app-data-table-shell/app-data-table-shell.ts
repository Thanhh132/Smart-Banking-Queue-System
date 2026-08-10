import { Component, input } from '@angular/core';

import { AppEmptyState } from '../app-empty-state/app-empty-state';
import { AppLoadingState } from '../app-loading-state/app-loading-state';

@Component({
  selector: 'app-data-table-shell',
  standalone: true,
  imports: [AppEmptyState, AppLoadingState],
  templateUrl: './app-data-table-shell.html',
  styleUrl: './app-data-table-shell.scss',
})
export class AppDataTableShell {
  readonly title = input('');
  readonly description = input('');
  readonly loading = input(false);
  readonly loadingLabel = input('Đang tải dữ liệu...');
  readonly empty = input(false);
  readonly emptyTitle = input('Chưa có dữ liệu');
  readonly emptyMessage = input('Không có dữ liệu phù hợp để hiển thị.');
}
