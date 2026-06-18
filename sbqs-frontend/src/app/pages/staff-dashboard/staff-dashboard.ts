import {
  ChangeDetectorRef,
  Component,
  inject,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';

import { DashboardLayout } from '../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppPageHeader } from '../../shared/components/app-page-header/app-page-header';
import { AppButton } from '../../shared/components/app-button/app-button';
import { AppCard } from '../../shared/components/app-card/app-card';

import { StaffService } from '../../core/services/staff.service';

@Component({
  selector: 'app-staff-dashboard',
  imports: [
    CommonModule,
    DashboardLayout,
    AppPageHeader,
    AppButton,
    AppCard
  ],
  templateUrl: './staff-dashboard.html',
  styleUrl: './staff-dashboard.scss',
})
export class StaffDashboard implements OnInit {

  private staffService = inject(StaffService);
  private cdr = inject(ChangeDetectorRef);

  currentTicket: any = null;
  errorMessage = '';

  counterId = 1;

  ngOnInit(): void {
    this.loadCurrentCounter();
  }

  loadCurrentCounter(): void {
    this.staffService
      .getCounters()
      .subscribe({
        next: (counters: any[]) => {
          const counter = counters.find(
            c => c.counterId === this.counterId
          );

          if (counter && counter.currentTicket) {
            this.currentTicket = counter.currentTicket;
          } else {
            this.currentTicket = null;
          }

          this.cdr.detectChanges();
        },
        error: (err) => {
          this.errorMessage = 'Không tải được trạng thái quầy';
          this.cdr.detectChanges();
          console.error(err);
        }
      });
  }

  callNext(): void {
    this.errorMessage = '';

    this.staffService
      .callNext(this.counterId)
      .subscribe({
        next: (ticket: any) => {
          this.currentTicket = ticket;
          this.cdr.detectChanges();
          console.log(ticket);
        },
        error: (err) => {
          this.errorMessage =
            err.error?.message || 'Không gọi được khách tiếp theo';
          this.cdr.detectChanges();
          console.error(err);
        }
      });
  }

  complete(): void {
    if (!this.currentTicket) {
      return;
    }

    this.errorMessage = '';

    this.staffService
      .complete(this.currentTicket.ticketId)
      .subscribe({
        next: () => {
          this.currentTicket = null;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.errorMessage =
            err.error?.message || 'Không hoàn thành được ticket';
          this.cdr.detectChanges();
          console.error(err);
        }
      });
  }
}