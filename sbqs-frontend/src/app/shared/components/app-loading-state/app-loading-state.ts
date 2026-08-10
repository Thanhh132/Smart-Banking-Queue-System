import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loading-state',
  standalone: true,
  templateUrl: './app-loading-state.html',
  styleUrl: './app-loading-state.scss',
})
export class AppLoadingState {
  readonly label = input('Đang tải dữ liệu...');
}
