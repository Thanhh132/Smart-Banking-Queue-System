import { NgFor } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

interface SidebarItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgFor],
  templateUrl: './app-sidebar.html',
  styleUrl: './app-sidebar.scss',
})
export class AppSidebar {
  private authService = inject(AuthService);
  private router = inject(Router);

  private menusByRole: Record<string, SidebarItem[]> = {
    SUPER_ADMIN: [
      { label: 'Tổng quan', icon: 'SA', route: '/super-admin' },
      { label: 'Chi nhánh', icon: 'BR', route: '/super-admin/branches' },
    ],
    BRANCH_ADMIN: [
      { label: 'Tổng quan', icon: 'DB', route: '/admin' },
      { label: 'Vận hành', icon: 'OP', route: '/admin/operations' },
      { label: 'Dịch vụ', icon: 'SV', route: '/admin/services' },
      { label: 'Gán dịch vụ', icon: 'MP', route: '/admin/mappings' },
      { label: 'Nhân viên', icon: 'ST', route: '/admin/users' },
      { label: 'Màn hình hàng đợi', icon: 'QM', route: '/monitor' },
    ],
    STAFF: [
      { label: 'Quầy phục vụ', icon: 'SD', route: '/staff' },
      { label: 'Màn hình hàng đợi', icon: 'QM', route: '/monitor' },
    ],
    CUSTOMER: [
      { label: 'Tổng quan', icon: 'CU', route: '/customer' },
      { label: 'Lấy số', icon: 'BR', route: '/branches' },
      { label: 'Phiếu của tôi', icon: 'TK', route: '/ticket' },
    ],
  };

  get menuItems(): SidebarItem[] {
    return this.menusByRole[this.authService.getRole()] || [];
  }

  logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
