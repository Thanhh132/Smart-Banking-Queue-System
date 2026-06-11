import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-ticket-result',
  imports: [],
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