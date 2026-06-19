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
      { label: 'System', icon: 'SA', route: '/super-admin' },
      { label: 'Branches', icon: 'BR', route: '/super-admin/branches' },
    ],
    BRANCH_ADMIN: [
      { label: 'Dashboard', icon: 'DB', route: '/admin' },
      { label: 'Operations', icon: 'OP', route: '/admin/operations' },
      { label: 'Services', icon: 'SV', route: '/admin/services' },
      { label: 'Mappings', icon: 'MP', route: '/admin/mappings' },
      { label: 'Staff', icon: 'ST', route: '/admin/users' },
      { label: 'Queue Monitor', icon: 'QM', route: '/monitor' },
    ],
    STAFF: [
      { label: 'Staff Desk', icon: 'SD', route: '/staff' },
      { label: 'Queue Monitor', icon: 'QM', route: '/monitor' },
    ],
    CUSTOMER: [
      { label: 'Customer', icon: 'CU', route: '/customer' },
      { label: 'Branches', icon: 'BR', route: '/branches' },
      { label: 'Ticket', icon: 'TK', route: '/ticket' },
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
