import { Component, Input } from '@angular/core';
import { AppSidebar } from '../../components/app-sidebar/app-sidebar';
import { AppTopbar } from '../../components/app-topbar/app-topbar';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [AppSidebar, AppTopbar],
  templateUrl: './dashboard-layout.html',
  styleUrl: './dashboard-layout.scss'
})
export class DashboardLayout {
  @Input() title = 'Dashboard';
}