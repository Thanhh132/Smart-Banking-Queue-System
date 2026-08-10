import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { AppIcon } from '../app-icon/app-icon';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink, AppIcon],
  templateUrl: './app-topbar.html',
  styleUrl: './app-topbar.scss'
})
export class AppTopbar {
  private authService = inject(AuthService);
  private router = inject(Router);

  @Input() title = 'Tổng quan';
  @Input() username = sessionStorage.getItem('fullName') || 'Người dùng';
  @Output() sidebarToggle = new EventEmitter<void>();

  get subtitle(): string {
    return sessionStorage.getItem('userRole') === 'CUSTOMER'
      ? 'Lấy số và theo dõi lượt giao dịch của bạn'
      : 'Theo dõi và xử lý công việc tại chi nhánh';
  }

  get roleLabel(): string {
    const role = sessionStorage.getItem('userRole');

    switch (role) {
      case 'SUPER_ADMIN':
        return 'Quản trị hệ thống';
      case 'BRANCH_ADMIN':
        return 'Quản trị chi nhánh';
      case 'STAFF':
        return 'Nhân viên';
      case 'CUSTOMER':
        return 'Khách hàng';
      default:
        return 'Người dùng';
    }
  }

  get homeRoute(): string {
    switch (sessionStorage.getItem('userRole')) {
      case 'SUPER_ADMIN':
        return '/super-admin';
      case 'BRANCH_ADMIN':
        return '/admin';
      case 'STAFF':
        return '/staff';
      case 'CUSTOMER':
        return '/customer';
      default:
        return '/';
    }
  }

  toggleSidebar(): void {
    this.sidebarToggle.emit();
  }

  logout(): void {
    this.authService.logout().subscribe();
    this.router.navigateByUrl('/login');
  }
}
