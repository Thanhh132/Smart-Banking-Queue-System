import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgFor } from '@angular/common';

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
  styleUrl: './app-sidebar.scss'
})
export class AppSidebar {
  menuItems: SidebarItem[] = [
    {
      label: 'Dashboard',
      icon: '📊',
      route: '/admin'
    },
    {
      label: 'Users',
      icon: '👥',
      route: '/admin/users'
    },
    {
      label: 'Services',
      icon: '🧾',
      route: '/admin/services'
    },
    {
      label: 'Mappings',
      icon: '🔗',
      route: '/admin/mappings'
    },
    {
      label: 'Queue Monitor',
      icon: '📺',
      route: '/monitor'
    }
  ];
}