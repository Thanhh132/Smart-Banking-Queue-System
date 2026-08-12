import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import {
  AdminOperationsService,
  BranchHours,
  CounterPayload,
  QueueMachinePayload,
} from '../../../core/services/admin-operations.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppButton } from '../../../shared/components/app-button/app-button';
import { AppCard } from '../../../shared/components/app-card/app-card';
import { AppConfirmDialog } from '../../../shared/components/app-confirm-dialog/app-confirm-dialog';
import { AppDataTableShell } from '../../../shared/components/app-data-table-shell/app-data-table-shell';
import { AppEmptyState } from '../../../shared/components/app-empty-state/app-empty-state';
import { AppLoadingState } from '../../../shared/components/app-loading-state/app-loading-state';
import { AppModalShell } from '../../../shared/components/app-modal-shell/app-modal-shell';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { AppStatusBadge } from '../../../shared/components/app-status-badge/app-status-badge';

type OperationsTab = 'hours' | 'machines' | 'counters' | 'assignments';
type DeleteTarget = { type: 'machine' | 'counter'; item: any };

@Component({
  selector: 'app-admin-operations',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DashboardLayout,
    AppButton,
    AppCard,
    AppConfirmDialog,
    AppDataTableShell,
    AppEmptyState,
    AppLoadingState,
    AppModalShell,
    AppPageHeader,
    AppStatusBadge,
  ],
  templateUrl: './admin-operations.html',
  styleUrl: './admin-operations.scss',
})
export class AdminOperations implements OnInit {
  private operationsService = inject(AdminOperationsService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  activeTab: OperationsTab = 'hours';
  isMachineModalOpen = false;
  isCounterBatchOpen = false;
  isCounterModalOpen = false;
  pendingDelete: DeleteTarget | null = null;
  isDeleting = false;

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
  isLoadingHours = false;
  isSubmittingMachine = false;
  isSubmittingCounter = false;
  updatingCounterId: number | null = null;
  successMessage = '';
  errorMessage = '';
  isSavingHours = false;
  branchHours: BranchHours[] = [];
  readonly dayNames = [
    '',
    'Thứ Hai',
    'Thứ Ba',
    'Thứ Tư',
    'Thứ Năm',
    'Thứ Sáu',
    'Thứ Bảy',
    'Chủ Nhật',
  ];

  get deleteDialogTitle(): string {
    return this.pendingDelete?.type === 'machine' ? 'Xóa máy bốc số' : 'Xóa quầy giao dịch';
  }

  get deleteDialogMessage(): string {
    if (this.pendingDelete?.type === 'machine') {
      const name = this.pendingDelete.item?.machineName || 'máy bốc số này';
      return `Xóa hẳn máy bốc số “${name}”?`;
    }

    const name = this.pendingDelete?.item?.counterName || 'quầy này';
    return `Xóa hẳn quầy “${name}”?`;
  }

  get machineFormTitle(): string {
    return this.editingMachineId ? 'Sửa máy bốc số' : 'Thêm máy bốc số';
  }

  get counterNumbersPreview(): string {
    const explicitNumbers = this.parseCounterNumbers();
    const numbers = explicitNumbers.length
      ? explicitNumbers
      : Array.from({ length: Math.max(1, Number(this.counterCount) || 1) }).map((_, index) =>
          String((Number(this.counterStartNumber) || 1) + index),
        );

    return numbers.slice(0, 6).join(', ') + (numbers.length > 6 ? '...' : '');
  }

  ngOnInit(): void {
    if (!this.ensureBranch()) {
      return;
    }

    this.loadOperations();
    this.loadBranchHours();
  }

  setActiveTab(tab: OperationsTab): void {
    this.activeTab = tab;
  }

  loadBranchHours(): void {
    if (!this.ensureBranch()) return;
    this.isLoadingHours = true;
    this.operationsService.getBranchHours(this.branchId).subscribe({
      next: (hours) => {
        this.branchHours = hours;
        this.isLoadingHours = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không tải được giờ làm việc.');
        this.isLoadingHours = false;
        this.cdr.detectChanges();
      },
    });
  }

  saveBranchHours(): void {
    this.isSavingHours = true;
    this.operationsService.updateBranchHours(this.branchHours).subscribe({
      next: (hours) => {
        this.branchHours = hours;
        this.isSavingHours = false;
        this.successMessage = 'Đã cập nhật giờ phục vụ của chi nhánh.';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không lưu được giờ làm việc.');
        this.isSavingHours = false;
        this.cdr.detectChanges();
      },
    });
  }

  applyWeekdayTemplate(): void {
    this.branchHours = this.branchHours.map((hours) =>
      hours.dayOfWeek <= 5
        ? {
            ...hours,
            closed: false,
            morningOpen: '08:00',
            morningClose: '12:00',
            afternoonOpen: '13:00',
            afternoonClose: '17:00',
          }
        : {
            ...hours,
            closed: true,
            morningOpen: null,
            morningClose: null,
            afternoonOpen: null,
            afternoonClose: null,
          },
    );
  }

  toggleDay(hours: BranchHours): void {
    hours.closed = !hours.closed;
    if (!hours.closed && !hours.morningOpen && !hours.afternoonOpen) {
      hours.morningOpen = '08:00';
      hours.morningClose = '12:00';
      hours.afternoonOpen = '13:00';
      hours.afternoonClose = '17:00';
    }
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
          (machine) => machine.branch?.branchId === this.branchId,
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
        this.isMachineModalOpen = false;
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
    this.isMachineModalOpen = true;
    this.cdr.detectChanges();
  }

  openCreateMachineModal(): void {
    this.resetMachineForm();
    this.isMachineModalOpen = true;
  }

  closeMachineModal(): void {
    if (!this.isSubmittingMachine) {
      this.isMachineModalOpen = false;
      this.resetMachineForm();
    }
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
        this.isMachineModalOpen = false;
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
        this.isCounterBatchOpen = false;
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
    this.isCounterModalOpen = true;
    this.cdr.detectChanges();
  }

  openCounterBatch(): void {
    this.isCounterBatchOpen = true;
  }

  closeCounterBatch(): void {
    if (!this.isSubmittingCounter) {
      this.isCounterBatchOpen = false;
    }
  }

  closeCounterModal(): void {
    if (!this.isSubmittingCounter) {
      this.isCounterModalOpen = false;
      this.resetCounterForm();
    }
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
        this.isCounterModalOpen = false;
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
        this.successMessage = queueMachineId
          ? 'Đã gán máy bốc số vào quầy.'
          : 'Đã gỡ máy khỏi quầy.';
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
    this.pendingDelete = { type: 'machine', item: machine };
  }

  deleteCounter(counter: any): void {
    this.pendingDelete = { type: 'counter', item: counter };
  }

  cancelDelete(): void {
    if (!this.isDeleting) {
      this.pendingDelete = null;
    }
  }

  confirmDelete(): void {
    if (!this.pendingDelete || this.isDeleting) {
      return;
    }

    this.isDeleting = true;
    const target = this.pendingDelete;

    if (target.type === 'machine') {
      this.operationsService.deleteQueueMachine(target.item.queueMachineId).subscribe({
        next: () => {
          this.successMessage = 'Đã xóa máy bốc số.';
          this.pendingDelete = null;
          this.isDeleting = false;
          this.loadQueueMachines();
        },
        error: (err) => {
          this.errorMessage = this.apiError.getMessage(
            err,
            'Không xóa được máy bốc số. Hãy gỡ liên kết, quầy hoặc phiếu liên quan trước.',
          );
          this.pendingDelete = null;
          this.isDeleting = false;
          this.cdr.detectChanges();
        },
      });
      return;
    }

    this.operationsService.deleteCounter(target.item.counterId).subscribe({
      next: () => {
        this.successMessage = 'Đã xóa quầy.';
        this.pendingDelete = null;
        this.isDeleting = false;
        this.loadCounters();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không xóa được quầy. Hãy hoàn tất phiếu đang gắn với quầy trước.',
        );
        this.pendingDelete = null;
        this.isDeleting = false;
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
      this.isLoadingHours = false;
      this.isLoadingMachines = false;
      this.isLoadingCounters = false;
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
