import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';

import { CustomerLiveTrackingService } from '../../../core/services/customer-live-tracking.service';
import { AuthService } from '../../../core/services/auth.service';
import { AppIcon } from '../app-icon/app-icon';

@Component({
  selector: 'app-live-ticket-notice',
  standalone: true,
  imports: [CommonModule, AppIcon],
  templateUrl: './live-ticket-notice.html',
  styleUrl: './live-ticket-notice.scss',
})
export class LiveTicketNotice {
  private authService = inject(AuthService);
  readonly liveTracking = inject(CustomerLiveTrackingService);
  readonly isCustomer = this.authService.getRole() === 'CUSTOMER';
}
