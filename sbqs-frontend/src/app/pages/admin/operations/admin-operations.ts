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
import { AppIcon } from '../../../shared/components/app-icon/app-icon';

@Component({
  selector: 'app-admin-operations',
  standalone: true,
  imports: [CommonModule, FormsModule, DashboardLayout, AppIcon],
  templateUrl: './admin-operations.html',
  styleUrl: './admin-operations.scss',
})
export class AdminOperations implements OnInit {
  private operationsService = inject(AdminOperationsService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  branchId = Number(sessionStorage.getItem('selectedBranchId')) || null;
  queueMachines: any[] = [];
  counters: any[] = [];

  machineCode = '';
  machineName = '';
  machineNote = '';

  counterCount = 1;
  counterStartNumber = 1;
  counterNamePrefix = 'Quầy';
  counterCodePrefix = 'Q';
  counterNumbersText = '';
  selectedMachineForCounters: number | null = null;

  editingMachineId: number | null = null;
  editingCounterId: number | null = null;
  counterForm = {
    counterCode: '',
    counterName: '',
    queueMachineId: null as number | null,
    status: 'INACTIVE',
  };

  isLoadingMachines = false;
  isLoadingCounters = false;
  isSubmittingMachine = false;
  isSubmittingCounter = false;
  updatingCounterId: number | null = null;
  successMessage = '';
  errorMessage = '';

  get machineFormTitle(): string {
    return this.editingMachineId ? 'Sửa máy bốc số' : 'Thêm máy bốc số';
  }

  get counterNumbersPreview(): string {
    const explicitNumbers = this.parseCounterNumbers();
    const numbers = explicitNumbers.length
      ? explicitNumbers
      : Array.from({ length: Math.max(1, Number(this.counterCount) || 1) }).map(
          (_, index) => String((Number(this.counterStartNumber) || 1) + index)
        );

    return numbers.slice(0, 6).join(', ') + (numbers.length > 6 ? '...' : '');
  }

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
        this.errorMessage = this.apiError.getMessage(err, 'Không tải được danh sách máy bốc số.');
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
        this.errorMessage = this.apiError.getMessage(err, 'Không tải được danh sách quầy.');
        this.isLoadingCounters = false;
        this.cdr.detectChanges();
      },
    });
  }

  saveMachine(): void {
    if (this.editingMachineId) {
      this.updateMachine();
      return;
    }

    this.quickCreateMachine();
  }

  quickCreateMachine(): void {
    if (!this.ensureBranch()) {
      return;
    }

    const nextNumber = this.queueMachines.length + 1;
    const payload: QueueMachinePayload = {
      machineCode: this.machineCode.trim() || `QM-${this.branchId}-${nextNumber}`,
      machineName: this.machineName.trim() || `Máy bốc số ${nextNumber}`,
      locationNote: this.machineNote,
      instructionNote: 'Chọn dịch vụ và nhận số thứ tự',
      status: 'ACTIVE',
      branch: { branchId: this.branchId },
    };

    this.isSubmittingMachine = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.operationsService.createQueueMachine(payload).subscribe({
      next: (machine) => {
        this.successMessage = 'Đã tạo máy bốc số.';
        this.resetMachineForm();
        this.selectedMachineForCounters = machine.queueMachineId;
        this.isSubmittingMachine = false;
        this.loadQueueMachines();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không tạo được máy bốc số.');
        this.isSubmittingMachine = false;
        this.cdr.detectChanges();
      },
    });
  }

  startEditMachine(machine: any): void {
    this.editingMachineId = machine.queueMachineId;
    this.machineCode = machine.machineCode || '';
    this.machineName = machine.machineName || '';
    this.machineNote = machine.locationNote || '';
    this.successMessage = '';
    this.errorMessage = '';
    this.cdr.detectChanges();
  }

  updateMachine(): void {
    if (!this.ensureBranch() || !this.editingMachineId) {
      return;
    }

    if (!this.machineCode.trim() || !this.machineName.trim()) {
      this.errorMessage = 'Mã máy và tên máy không được để trống.';
      return;
    }

    const payload: QueueMachinePayload = {
      machineCode: this.machineCode.trim(),
      machineName: this.machineName.trim(),
      locationNote: this.machineNote,
      instructionNote: 'Chọn dịch vụ và nhận số thứ tự',
      status: 'ACTIVE',
      branch: { branchId: this.branchId },
    };

    this.isSubmittingMachine = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.operationsService.updateQueueMachine(this.editingMachineId, payload).subscribe({
      next: () => {
        this.successMessage = 'Đã cập nhật máy bốc số.';
        this.resetMachineForm();
        this.isSubmittingMachine = false;
        this.loadQueueMachines();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không cập nhật được máy bốc số.');
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
      this.errorMessage = 'Hãy tạo hoặc chọn một máy bốc số trước khi tạo quầy.';
      return;
    }

    const explicitNumbers = this.parseCounterNumbers();
    const count = explicitNumbers.length || Math.max(1, Number(this.counterCount) || 1);
    const startNumber = Number(this.counterStartNumber) || 1;
    const numbers = explicitNumbers.length
      ? explicitNumbers
      : Array.from({ length: count }).map((_, index) => String(startNumber + index));

    const requests = numbers.map((number) => {
      const normalizedNumber = String(number).trim();
      const payload: CounterPayload = {
        counterCode: `${this.counterCodePrefix.trim() || 'Q'}-${this.branchId}-${this.normalizeCodePart(normalizedNumber)}`,
        counterName: `${this.counterNamePrefix.trim() || 'Quầy'} ${normalizedNumber}`,
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
        this.successMessage = `Đã tạo ${count} quầy.`;
        this.counterNumbersText = '';
        this.counterStartNumber = startNumber + count;
        this.isSubmittingCounter = false;
        this.loadCounters();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không tạo được quầy.');
        this.isSubmittingCounter = false;
        this.cdr.detectChanges();
      },
    });
  }

  startEditCounter(counter: any): void {
    this.editingCounterId = counter.counterId;
    this.counterForm = {
      counterCode: counter.counterCode || '',
      counterName: counter.counterName || '',
      queueMachineId: counter.queueMachine?.queueMachineId || null,
      status: counter.status || 'INACTIVE',
    };
    this.successMessage = '';
    this.errorMessage = '';
    this.cdr.detectChanges();
  }

  updateCounter(): void {
    if (!this.ensureBranch() || !this.editingCounterId) {
      return;
    }

    if (!this.counterForm.counterCode.trim() || !this.counterForm.counterName.trim()) {
      this.errorMessage = 'Mã quầy và tên quầy không được để trống.';
      return;
    }

    const payload: CounterPayload = {
      counterCode: this.counterForm.counterCode.trim(),
      counterName: this.counterForm.counterName.trim(),
      status: this.counterForm.status,
      branch: { branchId: this.branchId },
      queueMachine: this.counterForm.queueMachineId
        ? { queueMachineId: Number(this.counterForm.queueMachineId) }
        : null,
    };

    this.isSubmittingCounter = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.operationsService.updateCounter(this.editingCounterId, payload).subscribe({
      next: () => {
        this.successMessage = 'Đã cập nhật quầy.';
        this.resetCounterForm();
        this.isSubmittingCounter = false;
        this.loadCounters();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không cập nhật được quầy.');
        this.isSubmittingCounter = false;
        this.cdr.detectChanges();
      },
    });
  }

  assignQueueMachine(counter: any, queueMachineId: number | null): void {
    const payload: CounterPayload = {
      counterCode: counter.counterCode,
      counterName: counter.counterName,
      status: counter.status,
      branch: { branchId: this.branchId! },
      queueMachine: queueMachineId ? { queueMachineId: Number(queueMachineId) } : null,
    };
    this.updatingCounterId = counter.counterId;
    this.errorMessage = '';
    this.operationsService.updateCounter(counter.counterId, payload).subscribe({
      next: (saved) => {
        const index = this.counters.findIndex((item) => item.counterId === saved.counterId);
        if (index >= 0) this.counters[index] = saved;
        this.successMessage = queueMachineId ? 'Đã gán máy bốc số vào quầy.' : 'Đã gỡ máy khỏi quầy.';
        this.updatingCounterId = null;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không cập nhật được máy của quầy.');
        this.updatingCounterId = null;
        this.loadCounters();
      },
    });
  }

  deleteMachine(machine: any): void {
    if (!confirm(`Xóa hẳn máy bốc số "${machine.machineName}"?`)) {
      return;
    }

    this.operationsService.deleteQueueMachine(machine.queueMachineId).subscribe({
      next: () => {
        this.successMessage = 'Đã xóa máy bốc số.';
        this.loadQueueMachines();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không xóa được máy bốc số. Hãy gỡ liên kết, quầy hoặc phiếu liên quan trước.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  deleteCounter(counter: any): void {
    if (!confirm(`Xóa hẳn quầy "${counter.counterName}"?`)) {
      return;
    }

    this.operationsService.deleteCounter(counter.counterId).subscribe({
      next: () => {
        this.successMessage = 'Đã xóa quầy.';
        this.loadCounters();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không xóa được quầy. Hãy hoàn tất phiếu đang gắn với quầy trước.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  resetMachineForm(): void {
    this.editingMachineId = null;
    this.machineCode = '';
    this.machineName = '';
    this.machineNote = '';
    this.cdr.detectChanges();
  }

  resetCounterForm(): void {
    this.editingCounterId = null;
    this.counterForm = {
      counterCode: '',
      counterName: '',
      queueMachineId: null,
      status: 'INACTIVE',
    };
    this.cdr.detectChanges();
  }

  private ensureBranch(): this is this & { branchId: number } {
    if (!this.branchId) {
      this.errorMessage =
        'Tài khoản quản trị này chưa được gán chi nhánh. Hãy dùng tài khoản do quản trị viên hệ thống cấp.';
      this.cdr.detectChanges();
      return false;
    }

    return true;
  }

  private parseCounterNumbers(): string[] {
    return this.counterNumbersText
      .split(/[\n,;]+/)
      .map((item) => item.trim())
      .filter(Boolean);
  }

  private normalizeCodePart(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-zA-Z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .toUpperCase();
  }
}
