import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [],
  templateUrl: './app-page-header.html',
  styleUrl: './app-page-header.scss'
})
export class AppPageHeader {
  @Input() title = '';
  @Input() subtitle = '';
}