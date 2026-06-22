import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [],
  templateUrl: './app-topbar.html',
  styleUrl: './app-topbar.scss'
})
export class AppTopbar {
  @Input() title = 'Tổng quan';
  @Input() username = localStorage.getItem('fullName') || 'Người dùng SBQS';

  get subtitle(): string {
    return localStorage.getItem('userRole') === 'CUSTOMER'
      ? 'Lấy số và theo dõi lượt giao dịch của bạn'
      : 'Quản lý hệ thống SBQS';
  }

  get roleLabel(): string {
    const role = localStorage.getItem('userRole');

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
}
