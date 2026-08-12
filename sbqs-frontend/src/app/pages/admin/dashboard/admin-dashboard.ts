import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppButton } from '../../../shared/components/app-button/app-button';
import { AppCard } from '../../../shared/components/app-card/app-card';
import { ReportExportButtons } from '../../../shared/components/report-export-buttons/report-export-buttons';
import { AppIcon, AppIconName } from '../../../shared/components/app-icon/app-icon';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';

interface DashboardAction {
  label: string;
  title: string;
  description: string;
  route: string;
  icon: AppIconName;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    DashboardLayout,
    AppButton,
    AppCard,
    AppIcon,
    AppPageHeader,
    ReportExportButtons,
  ],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
})
export class AdminDashboard {
  fullName = sessionStorage.getItem('fullName') || 'Quản trị viên chi nhánh';

  readonly actions: DashboardAction[] = [
    {
      label: 'Vận hành',
      title: 'Máy bốc số và quầy',
      description: 'Thiết lập giờ hoạt động, máy bốc số và quầy giao dịch.',
      route: '/admin/operations',
      icon: 'settings',
    },
    {
      label: 'Phiếu khai báo',
      title: 'Cấu hình dịch vụ',
      description: 'Thiết lập nội dung khách hàng cần khai báo trước khi lấy số.',
      route: '/admin/services',
      icon: 'briefcase',
    },
    {
      label: 'Gán dịch vụ',
      title: 'Liên kết máy bốc số',
      description: 'Chọn các dịch vụ được cung cấp tại từng máy bốc số.',
      route: '/admin/mappings',
      icon: 'link',
    },
    {
      label: 'Nhân viên',
      title: 'Quản lý tài khoản',
      description: 'Tạo và quản lý tài khoản giao dịch viên của chi nhánh.',
      route: '/admin/users',
      icon: 'users',
    },
    {
      label: 'Lịch sử phục vụ',
      title: 'Tra cứu giao dịch',
      description: 'Theo dõi kết quả phục vụ và các lượt khách không đến.',
      route: '/admin/history',
      icon: 'file-text',
    },
  ];
}
