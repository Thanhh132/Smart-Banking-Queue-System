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
  @Input() username = localStorage.getItem('fullName') || 'SBQS User';

  get roleLabel(): string {
    const role = localStorage.getItem('userRole');

    switch (role) {
      case 'SUPER_ADMIN':
        return 'Super Admin';
      case 'BRANCH_ADMIN':
        return 'Branch Admin';
      case 'STAFF':
        return 'Staff';
      case 'CUSTOMER':
        return 'Customer';
      default:
        return 'User';
    }
  }
}
