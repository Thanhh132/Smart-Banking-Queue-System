import {
  ChangeDetectorRef,
  Component,
  inject,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { AppCard } from '../../shared/components/app-card/app-card';
import { AppButton } from '../../shared/components/app-button/app-button';
import { FormsModule } from '@angular/forms';
import { AdminServicesService } from '../../core/services/admin-services.service';
import { DashboardLayout } from '../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppPageHeader } from '../../shared/components/app-page-header/app-page-header';

@Component({
  selector: 'app-admin-services',
  imports: [
    CommonModule,
    FormsModule,
    AppCard,
    AppButton,
    DashboardLayout,
    AppPageHeader
  ],
  templateUrl: './admin-services.html',
  styleUrl: './admin-services.scss',
})
export class AdminServices implements OnInit {

  private adminService = inject(AdminServicesService);

  private cdr = inject(ChangeDetectorRef);

  services: any[] = [];

  ngOnInit(): void {
    this.loadServices();
  }

  loadServices(): void {

    this.adminService
      .getAllServices()
      .subscribe({

        next: (data) => {

          this.services = data;

          this.cdr.detectChanges();
        },

        error: (err) => {

          console.error(err);
        }
      });
  }

  deleteService(id: number): void {

    if (!confirm('Xóa dịch vụ?')) {
      return;
    }

    this.adminService
      .deleteService(id)
      .subscribe({

        next: () => {

          this.loadServices();
        },

        error: (err) => {

          console.error(err);
        }
      });
  }

  newService = {
    serviceCode: '',
    serviceName: '',
    serviceType: 'BASIC',
    description: '',
    estimatedTime: 10,
    status: 'ACTIVE',
    branch: {
      branchId: 1
    }
  };

  createService(): void {

    this.adminService
      .createService(this.newService)
      .subscribe({

        next: () => {

          this.loadServices();

          this.newService = {
            serviceCode: '',
            serviceName: '',
            serviceType: 'BASIC',
            description: '',
            estimatedTime: 10,
            status: 'ACTIVE',
            branch: {
              branchId: 1
            }
          };

          this.cdr.detectChanges();
        },

        error: (err) => {

          console.error(err);
        }
      });
  }
}