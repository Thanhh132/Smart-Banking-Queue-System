import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { AppStatusBadge } from '../../../shared/components/app-status-badge/app-status-badge';

@Component({
  selector: 'app-customer',
  standalone: true,
  imports: [CommonModule, RouterLink, DashboardLayout, AppIcon, AppPageHeader, AppStatusBadge],
  templateUrl: './customer.html',
  styleUrl: './customer.scss',
})
export class Customer {
  fullName = sessionStorage.getItem('fullName') || 'Khách hàng';

  get activeTicket(): any | null {
    const stored = sessionStorage.getItem('currentTicket');
    if (!stored) return null;
    try {
      const ticket = JSON.parse(stored);
      return ['WAITING', 'SERVING'].includes(ticket?.status) ? ticket : null;
    } catch {
      return null;
    }
  }
}
