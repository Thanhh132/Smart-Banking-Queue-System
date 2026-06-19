import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import {
  AdminOperationsService,
  CounterPayload,
  QueueMachinePayload,
} from '../../../core/services/admin-operations.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({
  selector: 'app-admin-operations',
  standalone: true,
  imports: [CommonModule, FormsModule, DashboardLayout],
  templateUrl: './admin-operations.html',
  styleUrl: './admin-operations.scss',
})
export class AdminOperations implements OnInit {
  private operationsService = inject(AdminOperationsService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  branchId = Number(localStorage.getItem('selectedBranchId')) || null;
  queueMachines: any[] = [];
  counters: any[] = [];

  machineNote = '';
  counterCount = 1;
  selectedMachineForCounters: number | null = null;

  isLoadingMachines = false;
  isLoadingCounters = false;
  isSubmittingMachine = false;
  isSubmittingCounter = false;
  successMessage = '';
  errorMessage = '';

  ngOnInit(): void {
    if (!this.ensureBranch()) {
      return;
    }

    this.loadOperations();
  }

  loadOperations(): void {
    this.loadQueueMachines();
    this.loadCounters();
  }

  loadQueueMachines(): void {
    if (!this.ensureBranch()) {
      return;
    }

    this.isLoadingMachines = true;

    this.operationsService.getQueueMachines().subscribe({
      next: (machines) => {
        this.queueMachines = (machines || []).filter(
          (machine) => machine.branch?.branchId === this.branchId
        );

        if (!this.selectedMachineForCounters && this.queueMachines.length > 0) {
          this.selectedMachineForCounters = this.queueMachines[0].queueMachineId;
        }

        this.isLoadingMachines = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Khong tai duoc danh sach may boc so.');
        this.isLoadingMachines = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadCounters(): void {
    if (!this.ensureBranch()) {
      return;
    }

    this.isLoadingCounters = true;

    this.operationsService.getCounters(this.branchId).subscribe({
      next: (counters) => {
        this.counters = counters || [];
        this.isLoadingCounters = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Khong tai duoc danh sach quay.');
        this.isLoadingCounters = false;
        this.cdr.detectChanges();
      },
    });
  }

  quickCreateMachine(): void {
    if (!this.ensureBranch()) {
      return;
    }

    const nextNumber = this.queueMachines.length + 1;
    const code = `QM-${this.branchId}-${nextNumber}`;
    const payload: QueueMachinePayload = {
      machineCode: code,
      machineName: `May boc so ${nextNumber}`,
      locationNote: this.machineNote,
      instructionNote: 'Chon dich vu va nhan so thu tu',
      status: 'ACTIVE',
      branch: { branchId: this.branchId },
    };

    this.isSubmittingMachine = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.operationsService.createQueueMachine(payload).subscribe({
      next: (machine) => {
        this.successMessage = 'Da tao may boc so.';
        this.machineNote = '';
        this.selectedMachineForCounters = machine.queueMachineId;
        this.isSubmittingMachine = false;
        this.loadQueueMachines();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Khong tao duoc may boc so.');
        this.isSubmittingMachine = false;
        this.cdr.detectChanges();
      },
    });
  }

  quickCreateCounters(): void {
    if (!this.ensureBranch()) {
      return;
    }

    if (!this.selectedMachineForCounters) {
      this.errorMessage = 'Hay tao hoac chon mot may boc so truoc khi tao quay.';
      return;
    }

    const count = Math.max(1, Number(this.counterCount) || 1);
    const startNumber = this.counters.length + 1;
    const requests = Array.from({ length: count }).map((_, index) => {
      const number = startNumber + index;
      const payload: CounterPayload = {
        counterCode: `C-${this.branchId}-${number}`,
        counterName: `Quay ${number}`,
        status: 'INACTIVE',
        branch: { branchId: this.branchId },
        queueMachine: { queueMachineId: Number(this.selectedMachineForCounters) },
      };

      return this.operationsService.createCounter(payload);
    });

    this.isSubmittingCounter = true;
    this.successMessage = '';
    this.errorMessage = '';

    forkJoin(requests).subscribe({
      next: () => {
        this.successMessage = `Da tao ${count} quay.`;
        this.isSubmittingCounter = false;
        this.loadCounters();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Khong tao duoc quay.');
        this.isSubmittingCounter = false;
        this.cdr.detectChanges();
      },
    });
  }

  deleteMachine(machine: any): void {
    if (!confirm(`Xoa han may boc so "${machine.machineName}"?`)) {
      return;
    }

    this.operationsService.deleteQueueMachine(machine.queueMachineId).subscribe({
      next: () => {
        this.successMessage = 'Da xoa may boc so.';
        this.loadQueueMachines();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Khong xoa duoc may boc so. Hay go mapping/quay/ticket lien quan truoc.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  deleteCounter(counter: any): void {
    if (!confirm(`Xoa han quay "${counter.counterName}"?`)) {
      return;
    }

    this.operationsService.deleteCounter(counter.counterId).subscribe({
      next: () => {
        this.successMessage = 'Da xoa quay.';
        this.loadCounters();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Khong xoa duoc quay. Hay hoan tat ticket dang gan voi quay truoc.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  private ensureBranch(): this is this & { branchId: number } {
    if (!this.branchId) {
      this.errorMessage =
        'Tai khoan Branch Admin nay chua duoc gan chi nhanh. Hay dung tai khoan do Super Admin cap cho chi nhanh.';
      this.cdr.detectChanges();
      return false;
    }

    return true;
  }
}
