import {
  ChangeDetectorRef,
  Component,
  inject,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';

import { Service } from '../../../core/models/service.model';
import { ServicesService } from '../../../core/services/services.service';
import { Router } from '@angular/router';
import { TicketService } from '../../../core/services/ticket.service';
import { AppHeader } from '../../../shared/components/app-header/app-header';
import { AppButton } from '../../../shared/components/app-button/app-button';

@Component({
  selector: 'app-service-selection',
  imports: [
    CommonModule,
    AppHeader,
    AppButton
  ],
  templateUrl: './service-selection.html',
  styleUrl: './service-selection.scss',
})
export class ServiceSelection implements OnInit {

  private servicesService = inject(ServicesService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private ticketService =
    inject(TicketService);

  services: Service[] = [];

  errorMessage = '';

  ngOnInit(): void {

    const branchId =
      localStorage.getItem('selectedBranchId');

    if (!branchId) {

      this.errorMessage =
        'Chưa chọn chi nhánh';

      return;
    }

    this.servicesService
      .getServicesByBranch(Number(branchId))
      .subscribe({

        next: (data) => {

          this.services = data;

          this.cdr.detectChanges();

          console.log(data);
        },

        error: (err) => {

          this.errorMessage =
            'Không tải được danh sách dịch vụ';

          this.cdr.detectChanges();

          console.error(err);
        }

      });
  }

  selectService(serviceId: number) {

    const branchId =
      Number(
        localStorage.getItem(
          'selectedBranchId'
        )
      );

    this.ticketService
      .createTicket(
        branchId,
        serviceId
      )
      .subscribe({

        next: (ticket: any) => {

          localStorage.setItem(
            'currentTicket',
            JSON.stringify(ticket)
          );

          this.router.navigate([
            '/ticket'
          ]);
        },

        error: (err) => {

          console.error(err);
        }
      });
  }
}