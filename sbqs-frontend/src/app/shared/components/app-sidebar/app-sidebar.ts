import { NgFor } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { AppIcon, AppIconName } from '../app-icon/app-icon';

interface SidebarItem {
  label: string;
  icon: AppIconName;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgFor, AppIcon],
  templateUrl: './app-sidebar.html',
  styleUrl: './app-sidebar.scss',
})
export class AppSidebar {
  private authService = inject(AuthService);
  private router = inject(Router);

  private menusByRole: Record<string, SidebarItem[]> = {
    SUPER_ADMIN: [
      { label: 'Tổng quan', icon: 'dashboard', route: '/super-admin' },
      { label: 'Chi nhánh', icon: 'building', route: '/super-admin/branches' },
      { label: 'Danh mục dịch vụ', icon: 'list-checks', route: '/super-admin/services' },
    ],
    BRANCH_ADMIN: [
      { label: 'Tổng quan', icon: 'dashboard', route: '/admin' },
      { label: 'Vận hành', icon: 'settings', route: '/admin/operations' },
      { label: 'Phiếu khai báo', icon: 'briefcase', route: '/admin/services' },
      { label: 'Gán dịch vụ', icon: 'list-checks', route: '/admin/mappings' },
      { label: 'Nhân viên', icon: 'users', route: '/admin/users' },
      { label: 'Màn hình hàng đợi', icon: 'monitor', route: '/monitor' },
    ],
    STAFF: [
      { label: 'Quầy phục vụ', icon: 'briefcase', route: '/staff' },
      { label: 'Màn hình hàng đợi', icon: 'monitor', route: '/monitor' },
    ],
    CUSTOMER: [
      { label: 'Tổng quan', icon: 'dashboard', route: '/customer' },
      { label: 'Lấy số', icon: 'building', route: '/branches' },
      { label: 'Phiếu của tôi', icon: 'ticket', route: '/ticket' },
      { label: 'Ủy quyền giao dịch', icon: 'file-text', route: '/delegations' },
    ],
  };

  get menuItems(): SidebarItem[] {
    const roleItems = this.menusByRole[this.authService.getRole()] || [];
    return [...roleItems, { label: 'Tài khoản của tôi', icon: 'users', route: '/account' }];
  }

  logout(): void {
    this.authService.logout().subscribe();
    this.router.navigateByUrl('/login');
  }
}
