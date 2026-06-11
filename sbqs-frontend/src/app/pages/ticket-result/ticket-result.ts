import { Component, OnInit } from '@angular/core';
import { AppCard } from '../../shared/components/app-card/app-card';
import { AppHeader } from '../../shared/components/app-header/app-header';

@Component({
  selector: 'app-ticket-result',
  imports: [
    AppCard,
    AppHeader
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