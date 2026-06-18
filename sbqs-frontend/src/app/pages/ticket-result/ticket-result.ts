import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardLayout } from '../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppPageHeader } from '../../shared/components/app-page-header/app-page-header';
import { AppCard } from '../../shared/components/app-card/app-card';

@Component({
  selector: 'app-ticket-result',
  imports: [
    CommonModule,
    DashboardLayout,
    AppPageHeader,
    AppCard
  ],
  templateUrl: './ticket-result.html',
  styleUrl: './ticket-result.scss',
})
export class TicketResult implements OnInit {

  ticket: any = null;

  ngOnInit(): void {

    const data =
      localStorage.getItem('currentTicket');

    if (data) {
      this.ticket = JSON.parse(data);
    }
  }
}