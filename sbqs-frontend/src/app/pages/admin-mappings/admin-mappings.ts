import {
  ChangeDetectorRef,
  Component,
  inject,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { AppCard } from '../../shared/components/app-card/app-card';
import { AppButton } from '../../shared/components/app-button/app-button';
import { DashboardLayout } from '../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppPageHeader } from '../../shared/components/app-page-header/app-page-header';

import { AdminMappingsService } from '../../core/services/admin-mappings.service';

@Component({
  selector: 'app-admin-mappings',
  imports: [
    CommonModule,
    FormsModule,
    AppCard,
    AppButton,
    DashboardLayout,
    AppPageHeader
  ],
  templateUrl: './admin-mappings.html',
  styleUrl: './admin-mappings.scss',
})
export class AdminMappings implements OnInit {

  private mappingService = inject(AdminMappingsService);
  private cdr = inject(ChangeDetectorRef);

  queueMachines: any[] = [];
  services: any[] = [];
  mappings: any[] = [];

  selectedQueueMachineId: number | null = null;
  selectedServiceId: number | null = null;

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.mappingService.getQueueMachines().subscribe({
      next: (data) => {
        this.queueMachines = data;
        this.cdr.detectChanges();
      }
    });

    this.mappingService.getServices().subscribe({
      next: (data) => {
        this.services = data;
        this.cdr.detectChanges();
      }
    });

    this.loadMappings();
  }

  loadMappings(): void {
    this.mappingService.getMappings().subscribe({
      next: (data) => {
        this.mappings = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
      }
    });
  }

  createMapping(): void {
    if (!this.selectedQueueMachineId || !this.selectedServiceId) {
      return;
    }

    this.mappingService
      .createMapping(
        this.selectedQueueMachineId,
        this.selectedServiceId
      )
      .subscribe({
        next: () => {
          this.loadMappings();
        },
        error: (err) => {
          console.error(err);
        }
      });
  }

  deleteMapping(mapping: any): void {
    const queueMachineId =
      mapping.queueMachine.queueMachineId;

    const serviceId =
      mapping.service.serviceId;

    this.mappingService
      .deleteMapping(queueMachineId, serviceId)
      .subscribe({
        next: () => {
          this.loadMappings();
        },
        error: (err) => {
          console.error(err);
        }
      });
  }
}