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
  @Input() username = sessionStorage.getItem('fullName') || 'Người dùng';

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
}
