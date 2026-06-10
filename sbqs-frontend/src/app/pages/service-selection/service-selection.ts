import {
  ChangeDetectorRef,
  Component,
  inject,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';

import { Service } from '../../core/models/service.model';
import { ServicesService } from '../../core/services/services.service';

@Component({
  selector: 'app-service-selection',
  imports: [CommonModule],
  templateUrl: './service-selection.html',
  styleUrl: './service-selection.scss',
})
export class ServiceSelection implements OnInit {

  private servicesService = inject(ServicesService);
  private cdr = inject(ChangeDetectorRef);

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
}