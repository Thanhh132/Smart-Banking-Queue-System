import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import {
  AdminOperationsService,
  BranchHours,
} from '../../../core/services/admin-operations.service';
import { AdminOperations } from './admin-operations';

describe('AdminOperations', () => {
  let component: AdminOperations;
  let fixture: ComponentFixture<AdminOperations>;

  const hours: BranchHours[] = Array.from({ length: 7 }, (_, index) => ({
    dayOfWeek: index + 1,
    closed: index > 4,
    morningOpen: index > 4 ? null : '08:00',
    morningClose: index > 4 ? null : '12:00',
    afternoonOpen: index > 4 ? null : '13:00',
    afternoonClose: index > 4 ? null : '17:00',
  }));
  const machine = {
    queueMachineId: 3,
    machineCode: 'QM-8-1',
    machineName: 'Máy sảnh chính',
    locationNote: 'Tầng 1',
    status: 'ACTIVE',
    branch: { branchId: 8 },
  };
  const counter = {
    counterId: 5,
    counterCode: 'Q-8-1',
    counterName: 'Quầy 1',
    status: 'INACTIVE',
    queueMachine: machine,
  };

  const operationsService = {
    getBranchHours: vi.fn(() => of(hours)),
    updateBranchHours: vi.fn(() => of(hours)),
    getQueueMachines: vi.fn(() => of([machine])),
    createQueueMachine: vi.fn(() => of(machine)),
    updateQueueMachine: vi.fn(() => of(machine)),
    deleteQueueMachine: vi.fn(() => of('')),
    getCounters: vi.fn(() => of([counter])),
    createCounter: vi.fn((payload: unknown) => of(payload)),
    updateCounter: vi.fn(() => of(counter)),
    deleteCounter: vi.fn(() => of('')),
  };

  beforeEach(async () => {
    sessionStorage.setItem('selectedBranchId', '8');
    vi.clearAllMocks();
    operationsService.getBranchHours.mockReturnValue(of(hours.map((item) => ({ ...item }))));
    operationsService.updateBranchHours.mockReturnValue(of(hours));
    operationsService.getQueueMachines.mockReturnValue(
      of([machine, { ...machine, queueMachineId: 9, branch: { branchId: 9 } }]),
    );
    operationsService.createQueueMachine.mockReturnValue(of(machine));
    operationsService.updateQueueMachine.mockReturnValue(of(machine));
    operationsService.deleteQueueMachine.mockReturnValue(of(''));
    operationsService.getCounters.mockReturnValue(of([counter]));
    operationsService.createCounter.mockImplementation((payload: unknown) => of(payload));
    operationsService.updateCounter.mockReturnValue(of(counter));
    operationsService.deleteCounter.mockReturnValue(of(''));

    await TestBed.configureTestingModule({
      imports: [AdminOperations],
      providers: [
        { provide: AdminOperationsService, useValue: operationsService },
        {
          provide: ApiErrorService,
          useValue: { getMessage: vi.fn((_error: unknown, fallback: string) => fallback) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminOperations);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => sessionStorage.removeItem('selectedBranchId'));

  it('loads branch operations and presents the four focused work areas', async () => {
    expect(operationsService.getBranchHours).toHaveBeenCalledWith(8);
    expect(operationsService.getCounters).toHaveBeenCalledWith(8);
    expect(component.queueMachines).toEqual([machine]);
    expect(component.selectedMachineForCounters).toBe(3);
    expect(fixture.nativeElement.querySelectorAll('[role="tab"]')).toHaveLength(4);
    expect(fixture.nativeElement.querySelector('[role="tab"]:nth-child(2)').textContent).toContain(
      '1 máy',
    );
    expect(fixture.nativeElement.querySelector('[role="tab"]:nth-child(3)').textContent).toContain(
      '1 quầy',
    );
    expect(fixture.nativeElement.querySelectorAll('.hours-row')).toHaveLength(7);

    component.setActiveTab('machines');
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.machines-table')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.counters-table')).toBeFalsy();
  });

  it('keeps the hours template, day toggle and save payload', () => {
    component.branchHours[6].closed = false;
    component.applyWeekdayTemplate();
    expect(component.branchHours[6].closed).toBe(true);
    expect(component.branchHours[0].morningOpen).toBe('08:00');

    component.toggleDay(component.branchHours[6]);
    expect(component.branchHours[6]).toMatchObject({
      closed: false,
      morningOpen: '08:00',
      afternoonOpen: '13:00',
    });

    const expectedPayload = component.branchHours;
    component.saveBranchHours();
    expect(operationsService.updateBranchHours).toHaveBeenCalledWith(expectedPayload);
  });

  it('creates and edits machines through the modal without changing request contracts', () => {
    component.openCreateMachineModal();
    component.machineCode = 'QM-VIP';
    component.machineName = 'Máy ưu tiên';
    component.machineNote = 'Khu VIP';
    component.saveMachine();

    expect(operationsService.createQueueMachine).toHaveBeenCalledWith({
      machineCode: 'QM-VIP',
      machineName: 'Máy ưu tiên',
      locationNote: 'Khu VIP',
      instructionNote: 'Chọn dịch vụ và nhận số thứ tự',
      status: 'ACTIVE',
      branch: { branchId: 8 },
    });
    expect(component.isMachineModalOpen).toBe(false);

    component.startEditMachine(machine);
    component.machineName = 'Máy sảnh mới';
    component.saveMachine();
    expect(operationsService.updateQueueMachine).toHaveBeenCalledWith(
      3,
      expect.objectContaining({ machineCode: 'QM-8-1', machineName: 'Máy sảnh mới' }),
    );
  });

  it('renders machine editing in the shared modal shell', async () => {
    component.openCreateMachineModal();
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('app-modal-shell [role="dialog"]')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Thêm máy bốc số');
  });

  it('creates a counter batch from the existing explicit-number input', () => {
    component.openCounterBatch();
    component.counterCodePrefix = 'Q';
    component.counterNamePrefix = 'Quầy';
    component.counterNumbersText = '202, VIP';
    component.selectedMachineForCounters = 3;
    component.quickCreateCounters();

    expect(operationsService.createCounter).toHaveBeenNthCalledWith(1, {
      counterCode: 'Q-8-202',
      counterName: 'Quầy 202',
      status: 'INACTIVE',
      branch: { branchId: 8 },
      queueMachine: { queueMachineId: 3 },
    });
    expect(operationsService.createCounter).toHaveBeenNthCalledWith(
      2,
      expect.objectContaining({ counterCode: 'Q-8-VIP', counterName: 'Quầy VIP' }),
    );
    expect(component.isCounterBatchOpen).toBe(false);
  });

  it('keeps counter edit and machine assignment payloads', () => {
    component.startEditCounter(counter);
    component.counterForm.counterName = 'Quầy ưu tiên';
    component.updateCounter();

    expect(operationsService.updateCounter).toHaveBeenCalledWith(5, {
      counterCode: 'Q-8-1',
      counterName: 'Quầy ưu tiên',
      status: 'INACTIVE',
      branch: { branchId: 8 },
      queueMachine: { queueMachineId: 3 },
    });

    operationsService.updateCounter.mockClear();
    component.assignQueueMachine(counter, null);
    expect(operationsService.updateCounter).toHaveBeenCalledWith(5, {
      counterCode: 'Q-8-1',
      counterName: 'Quầy 1',
      status: 'INACTIVE',
      branch: { branchId: 8 },
      queueMachine: null,
    });
  });

  it('requires the shared confirmation dialog before deleting a machine', async () => {
    component.deleteMachine(machine);
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();

    expect(operationsService.deleteQueueMachine).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('app-confirm-dialog [role="dialog"]')).toBeTruthy();

    component.confirmDelete();
    expect(operationsService.deleteQueueMachine).toHaveBeenCalledWith(3);
    expect(component.pendingDelete).toBeNull();
  });

  it('requires the shared confirmation dialog before deleting a counter', () => {
    component.deleteCounter(counter);
    expect(operationsService.deleteCounter).not.toHaveBeenCalled();

    component.confirmDelete();
    expect(operationsService.deleteCounter).toHaveBeenCalledWith(5);
    expect(component.pendingDelete).toBeNull();
  });
});
