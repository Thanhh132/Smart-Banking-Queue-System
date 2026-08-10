import { Component, Input, OnDestroy, OnInit, inject, signal } from '@angular/core';
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
  readonly sidebarCollapsed = signal(false);
  readonly sidebarOpen = signal(false);
  @Input() title = 'Tổng quan';

  get isFallbackSession(): boolean {
    return this.authService.isFallbackSession();
  }

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

  toggleSidebar(): void {
    if (this.isMobileViewport()) {
      this.sidebarOpen.update((open) => !open);
      return;
    }

    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  toggleSidebarCollapse(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  closeMobileSidebar(): void {
    this.sidebarOpen.set(false);
  }

  private isMobileViewport(): boolean {
    return typeof window !== 'undefined' &&
      typeof window.matchMedia === 'function' &&
      window.matchMedia('(max-width: 991.98px)').matches;
  }
}
