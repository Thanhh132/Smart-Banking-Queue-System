import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({
  selector: 'app-customer',
  standalone: true,
  imports: [RouterLink, DashboardLayout],
  templateUrl: './customer.html',
  styleUrl: './customer.scss',
})
export class Customer {
  fullName = localStorage.getItem('fullName') || 'Khách hàng';
}
