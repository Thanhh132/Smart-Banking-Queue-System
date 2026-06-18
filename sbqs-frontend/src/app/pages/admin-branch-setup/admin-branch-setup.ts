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
import { AdminBranchSetupService } from '../../core/services/admin-branch-setup.service';

@Component({
  selector: 'app-admin-branch-setup',
  imports: [
    CommonModule,
    FormsModule,
    AppCard,
    AppButton,
    DashboardLayout,
    AppPageHeader
  ],
  templateUrl: './admin-branch-setup.html',
  styleUrl: './admin-branch-setup.scss',
})
export class AdminBranchSetup implements OnInit {
  selectedQueueMachineId: number | null = null;
  selectedServiceIds: number[] = [];

  private setupService = inject(AdminBranchSetupService);
  private cdr = inject(ChangeDetectorRef);

  branchId = 1;

  branch: any = null;
  queueMachines: any[] = [];
  services: any[] = [];
  counters: any[] = [];
  mappings: any[] = [];

  newQueueMachine = {
    machineCode: '',
    machineName: '',
    locationNote: '',
    instructionNote: '',
    status: 'ACTIVE',
    branch: {
      branchId: 1
    }
  };

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

  newCounter = {
    counterCode: '',
    counterName: '',
    status: 'ACTIVE',
    branch: {
      branchId: 1
    },
    queueMachine: {
      queueMachineId: null
    }
  };

  ngOnInit(): void {
    this.loadSetupData();
  }

  loadSetupData(): void {
    this.setupService.getBranch(this.branchId).subscribe({
      next: (branches: any[]) => {
        this.branch = branches.find(
          b => b.branchId === this.branchId
        );
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
      }
    });

    this.setupService.getQueueMachines().subscribe({
      next: (data: any[]) => {
        this.queueMachines = data.filter(
          m => m.branch?.branchId === this.branchId
        );
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
      }
    });

    this.setupService.getServicesByBranch(this.branchId).subscribe({
      next: (data: any[]) => {
        this.services = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
      }
    });

    this.setupService.getCountersByBranch(this.branchId).subscribe({
      next: (data: any[]) => {
        this.counters = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
      }
    });

    this.setupService.getMappings().subscribe({
      next: (data: any[]) => {
        this.mappings = data.filter(
          mapping =>
            mapping.queueMachine?.branch?.branchId === this.branchId
        );
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
      }
    });
  }

  createQueueMachine(): void {
    this.setupService
      .createQueueMachine(this.newQueueMachine)
      .subscribe({
        next: () => {
          this.loadSetupData();

          this.newQueueMachine = {
            machineCode: '',
            machineName: '',
            locationNote: '',
            instructionNote: '',
            status: 'ACTIVE',
            branch: {
              branchId: this.branchId
            }
          };

          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error(err);
        }
      });
  }

  createService(): void {
    this.setupService
      .createService(this.newService)
      .subscribe({
        next: () => {
          this.loadSetupData();

          this.newService = {
            serviceCode: '',
            serviceName: '',
            serviceType: 'BASIC',
            description: '',
            estimatedTime: 10,
            status: 'ACTIVE',
            branch: {
              branchId: this.branchId
            }
          };

          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error(err);
        }
      });
  }

  createMapping(): void {

    if (
      !this.selectedQueueMachineId ||
      this.selectedServiceIds.length === 0
    ) {
      return;
    }

    this.selectedServiceIds.forEach(serviceId => {

      this.setupService
        .createMapping(
          this.selectedQueueMachineId!,
          serviceId
        )
        .subscribe({
          next: () => {
            this.loadSetupData();
          },
          error: (err) => {
            console.error(err);
          }
        });

    });

    this.selectedServiceIds = [];
  }

  toggleService(serviceId: number): void {

    const index =
      this.selectedServiceIds.indexOf(serviceId);

    if (index > -1) {

      this.selectedServiceIds.splice(index, 1);

    } else {

      this.selectedServiceIds.push(serviceId);
    }
  }

  createCounter(): void {
    if (!this.newCounter.queueMachine.queueMachineId) {
      return;
    }

    this.setupService
      .createCounter(this.newCounter)
      .subscribe({
        next: () => {
          this.loadSetupData();

          this.newCounter = {
            counterCode: '',
            counterName: '',
            status: 'ACTIVE',
            branch: {
              branchId: this.branchId
            },
            queueMachine: {
              queueMachineId: null
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

