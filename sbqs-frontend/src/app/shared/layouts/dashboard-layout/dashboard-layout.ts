import { Component, Input, OnDestroy, OnInit, inject } from '@angular/core';
import { AppSidebar } from '../../components/app-sidebar/app-sidebar';
import { AppTopbar } from '../../components/app-topbar/app-topbar';
import { LiveTicketNotice } from '../../components/live-ticket-notice/live-ticket-notice';
import { AuthService } from '../../../core/services/auth.service';
import { CustomerLiveTrackingService } from '../../../core/services/customer-live-tracking.service';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [AppSidebar, AppTopbar, LiveTicketNotice],
  templateUrl: './dashboard-layout.html',
  styleUrl: './dashboard-layout.scss'
})
export class DashboardLayout implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private liveTracking = inject(CustomerLiveTrackingService);
  private customerTrackingStarted = false;
  @Input() title = 'Dashboard';

  ngOnInit(): void {
    if (this.authService.getRole() === 'CUSTOMER') {
      this.liveTracking.start();
      this.customerTrackingStarted = true;
    }
  }

  ngOnDestroy(): void {
    if (this.customerTrackingStarted) {
      this.liveTracking.stop();
    }
  }
}
