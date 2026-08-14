import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { switchMap } from 'rxjs';

import { QueueMonitor } from '../../../core/models/queue-monitor.model';
import { Branch } from '../../../core/models/branch.model';
import { FormFieldDefinition, Service } from '../../../core/models/service.model';
import { AccountService } from '../../../core/services/account.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { BranchService } from '../../../core/services/branch.service';
import { QueueMonitorService } from '../../../core/services/queue-monitor.service';
import { ServicesService } from '../../../core/services/services.service';
import { TicketService } from '../../../core/services/ticket.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppButton } from '../../../shared/components/app-button/app-button';
import { AppEmptyState } from '../../../shared/components/app-empty-state/app-empty-state';
import { AppLoadingState } from '../../../shared/components/app-loading-state/app-loading-state';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';

@Component({
  selector: 'app-service-selection',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DashboardLayout,
    AppButton,
    AppEmptyState,
    AppLoadingState,
    AppPageHeader,
  ],
  templateUrl: './service-selection.html',
  styleUrls: ['./service-selection.scss', './service-selection-profile.scss'],
})
export class ServiceSelection implements OnInit {
  private servicesService = inject(ServicesService);
  private branchService = inject(BranchService);
  private accountService = inject(AccountService);
  private monitorService = inject(QueueMonitorService);
  private ticketService = inject(TicketService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  services: Service[] = [];
  monitor: QueueMonitor | null = null;
  selectedBranch: Branch | null = null;
  selectedService: Service | null = null;
  transactionForm: FormGroup = this.fb.group({});
  profileForm: FormGroup = this.fb.group({
    fullName: [{ value: '', disabled: true }, Validators.required],
    phone: [{ value: '', disabled: true }, Validators.required],
    permanentAddress: ['', Validators.required],
    contactAddress: ['', Validators.required],
  });
  errorMessage = '';
  isLoading = true;
  isSubmitting = false;
  private profileValues: Record<string, string> = {};
  private pendingTicketRequestKey: string | null = null;

  get fields(): FormFieldDefinition[] {
    return this.selectedService?.formSchema || [];
  }
  get visibleFields(): FormFieldDefinition[] {
    return this.fields.filter((field) => !this.isProfileAlias(field.key));
  }
  get sections(): string[] {
    return [...new Set(this.visibleFields.map((field) => field.section || 'Thông tin giao dịch'))];
  }
  fieldsInSection(section: string): FormFieldDefinition[] {
    return this.visibleFields.filter(
      (field) => (field.section || 'Thông tin giao dịch') === section,
    );
  }
  serviceTypeLabel(type?: string): string {
    return type?.trim() || 'Dịch vụ tại quầy';
  }

  get selectedBranchName(): string {
    return this.selectedBranch?.branchName || this.monitor?.branchName || 'Chi nhánh đã chọn';
  }

  get selectedBranchAddress(): string {
    if (!this.selectedBranch) return '';
    return [
      this.selectedBranch.address,
      this.selectedBranch.ward,
      this.selectedBranch.district,
      this.selectedBranch.province,
    ]
      .filter((part) => !!part?.trim())
      .join(', ');
  }

  isServiceAvailable(service: Service): boolean {
    return service.status?.toUpperCase() === 'ACTIVE';
  }

  changeBranch(): void {
    this.router.navigate(['/branches']);
  }

  ngOnInit(): void {
    const branchId = Number(sessionStorage.getItem('selectedBranchId'));
    if (!branchId) {
      this.router.navigate(['/branches']);
      return;
    }
    this.branchService.getBranches().subscribe({
      next: (branches) => {
        this.selectedBranch =
          (branches || []).find((branch) => branch.branchId === branchId) || null;
        this.cdr.detectChanges();
      },
    });
    this.servicesService.getMappedServicesByBranch(branchId).subscribe({
      next: (services) => {
        this.services = this.sortServices(services || []);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.errorMessage = this.apiError.getMessage(error, 'Không tải được danh sách dịch vụ.');
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
    this.monitorService.getMonitor(branchId).subscribe({
      next: (monitor) => {
        this.monitor = monitor;
        this.cdr.detectChanges();
      },
    });
    this.accountService.getPaperlessProfile().subscribe({
      next: (profile) => {
        this.profileValues = profile.values || {};
        this.profileForm.patchValue({
          fullName: this.profileValues['FULL_NAME'] || '',
          phone: this.profileValues['MOBILE_PHONE'] || '',
          permanentAddress: this.profileValues['PERMANENT_ADDRESS'] || '',
          contactAddress: this.profileValues['CONTACT_ADDRESS'] || '',
        });
        if (this.selectedService) this.applyProfileDefaults();
      },
    });
  }

  private sortServices(services: Service[]): Service[] {
    return [...services].sort((left, right) =>
      left.serviceName.localeCompare(right.serviceName, 'vi'),
    );
  }

  /**
   * Dựng Reactive Form từ schema do backend cấu hình và chuyển về phiếu hiện tại nếu
   * khách đã có một lượt WAITING/SERVING, tránh tạo hành trình cấp số song song.
   */
  selectService(service: Service): void {
    if (!this.isServiceAvailable(service)) return;
    const current = sessionStorage.getItem('currentTicket');
    if (current) {
      try {
        if (['WAITING', 'SERVING'].includes(JSON.parse(current)?.status)) {
          this.router.navigate(['/ticket']);
          return;
        }
      } catch {
        sessionStorage.removeItem('currentTicket');
      }
    }
    this.selectedService = service;
    this.pendingTicketRequestKey = null;
    this.errorMessage = '';
    const controls: Record<string, unknown> = {};
    for (const field of service.formSchema || []) {
      const validators = field.required
        ? [field.type === 'CHECKBOX' ? Validators.requiredTrue : Validators.required]
        : [];
      controls[field.key] = [
        field.type === 'CHECKBOX' ? false : this.profileDefault(field.key),
        validators,
      ];
    }
    this.transactionForm = this.fb.group(controls);
    this.transactionForm.valueChanges.subscribe(() => {
      this.errorMessage = '';
      this.pendingTicketRequestKey = null;
    });
  }

  closeForm(): void {
    this.selectedService = null;
    this.pendingTicketRequestKey = null;
    this.transactionForm = this.fb.group({});
  }

  /**
   * Lưu phần hồ sơ dùng chung trước, rồi mới tạo giao dịch nháp và cấp số. switchMap
   * bảo đảm bước cấp phiếu không chạy nếu cập nhật hồ sơ thất bại.
   */
  submit(): void {
    if (
      !this.selectedService ||
      this.transactionForm.invalid ||
      this.profileForm.invalid ||
      this.isSubmitting
    ) {
      this.transactionForm.markAllAsTouched();
      this.profileForm.markAllAsTouched();
      if (this.profileForm.invalid) {
        this.errorMessage = 'Vui lòng bổ sung đầy đủ địa chỉ thường trú và nơi ở hiện tại.';
        return;
      }
      const missing = this.fields
        .filter((field) => this.transactionForm.get(field.key)?.invalid)
        .map((field) => field.label);
      this.errorMessage = missing.length
        ? `Vui lòng bổ sung: ${missing.join(', ')}.`
        : 'Vui lòng kiểm tra lại thông tin.';
      return;
    }
    const branchId = Number(sessionStorage.getItem('selectedBranchId'));
    this.isSubmitting = true;
    const profile = this.profileForm.getRawValue();
    const values = this.transactionValues(profile);
    const idempotencyKey = this.pendingTicketRequestKey || crypto.randomUUID();
    this.pendingTicketRequestKey = idempotencyKey;
    this.accountService
      .updatePaperlessProfile({
        serviceId: this.selectedService.serviceId,
        values: {
          PERMANENT_ADDRESS: profile.permanentAddress || '',
          CONTACT_ADDRESS: profile.contactAddress || '',
        },
      })
      .pipe(
        switchMap(() =>
          this.ticketService.createPreparedTicket(
            branchId,
            this.selectedService!.serviceId,
            values,
            idempotencyKey,
          ),
        ),
      )
      .subscribe({
        next: (ticket) => {
          this.pendingTicketRequestKey = null;
          sessionStorage.setItem('currentTicket', JSON.stringify(ticket));
          this.router.navigate(['/ticket']);
        },
        error: (error) => {
          if (error?.error?.code === 'ACTIVE_TICKET_EXISTS') {
            this.pendingTicketRequestKey = null;
            this.router.navigate(['/ticket']);
            return;
          }
          if (error?.status >= 400 && error.status < 500) {
            this.pendingTicketRequestKey = null;
          }
          this.errorMessage = this.apiError.getMessage(error, 'Không thể tạo giao dịch nháp.');
          this.isSubmitting = false;
          this.cdr.detectChanges();
        },
      });
  }

  isInvalid(key: string): boolean {
    const control = this.transactionForm.get(key);
    return !!(control?.touched && control.invalid);
  }

  formatAmount(value: unknown): string {
    const digits = String(value ?? '').replace(/\D/g, '');
    return digits ? digits.replace(/\B(?=(\d{3})+(?!\d))/g, ',') : '';
  }

  onAmountInput(key: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 18);
    input.value = this.formatAmount(digits);
    const control = this.transactionForm.get(key);
    control?.setValue(digits);
    control?.markAsDirty();
  }

  private applyProfileDefaults(): void {
    for (const field of this.fields) {
      const control = this.transactionForm.get(field.key);
      const value = this.profileDefault(field.key);
      if (control && !control.value && value) control.setValue(value, { emitEvent: false });
    }
    this.cdr.detectChanges();
  }

  private isProfileAlias(fieldKey: string): boolean {
    return ['fullname', 'accountholder', 'phone', 'address'].includes(fieldKey.toLowerCase());
  }

  /** Ghép các alias ẩn trong schema với hồ sơ thật trước khi gửi dữ liệu lên backend. */
  private transactionValues(
    profile: Record<string, string | null | undefined>,
  ): Record<string, unknown> {
    const values = { ...this.transactionForm.getRawValue() };
    const aliases: Record<string, string> = {
      fullname: profile['fullName'] || '',
      accountholder: profile['fullName'] || '',
      phone: profile['phone'] || '',
      address: profile['permanentAddress'] || '',
    };
    for (const field of this.fields) {
      const value = aliases[field.key.toLowerCase()];
      if (value !== undefined) values[field.key] = value;
    }
    return values;
  }

  private profileDefault(fieldKey: string): string {
    const normalizedFieldKey = fieldKey.toLowerCase();
    const profileKeys: Record<string, string[]> = {
      fullname: ['FULL_NAME'],
      accountholder: ['FULL_NAME'],
      cardholdername: ['FULL_NAME'],
      phone: ['MOBILE_PHONE'],
      email: ['EMAIL_ADDRESS'],
      oldidentitynumber: ['IDENTITY_NUMBER'],
      identitynumber: ['IDENTITY_NUMBER'],
      address: ['CONTACT_ADDRESS', 'PERMANENT_ADDRESS'],
      employer: ['EMPLOYER_NAME'],
      monthlyincome: ['MONTHLY_INCOME'],
    };
    return (
      (profileKeys[normalizedFieldKey] || [])
        .map((key) => this.profileValues[key])
        .find((value) => !!value?.trim()) || ''
    );
  }
}
