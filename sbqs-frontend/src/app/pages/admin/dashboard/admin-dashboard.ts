import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { ReportExportButtons } from '../../../shared/components/report-export-buttons/report-export-buttons';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, DashboardLayout, ReportExportButtons, AppIcon],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
})
export class AdminDashboard {
  fullName = localStorage.getItem('fullName') || 'Admin Branch';
}
