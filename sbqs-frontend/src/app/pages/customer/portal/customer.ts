import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-customer',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './customer.html',
  styleUrl: './customer.scss',
})
export class Customer {
  private authService = inject(AuthService);
  private router = inject(Router);

  fullName = localStorage.getItem('fullName') || 'Customer';

  logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
