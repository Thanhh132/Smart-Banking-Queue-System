import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [],
  templateUrl: './app-topbar.html',
  styleUrl: './app-topbar.scss'
})
export class AppTopbar {
  @Input() title = 'Dashboard';
  @Input() username = 'Branch Admin';
}