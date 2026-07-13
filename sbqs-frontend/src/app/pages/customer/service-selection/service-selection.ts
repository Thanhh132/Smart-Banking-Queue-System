import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { QueueMonitor } from '../../../core/models/queue-monitor.model';
import { FormFieldDefinition, Service } from '../../../core/models/service.model';
import { AccountService } from '../../../core/services/account.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { QueueMonitorService } from '../../../core/services/queue-monitor.service';
import { ServicesService } from '../../../core/services/services.service';
import { TicketService } from '../../../core/services/ticket.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({
  selector: 'app-service-selection',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DashboardLayout],
  templateUrl: './service-selection.html',
  styleUrl: './service-selection.scss',
})
export class ServiceSelection implements OnInit {
  private readonly serviceOrder = [
    'DEBIT_CARD_NEW', 'DEBIT_CARD_REISSUE', 'ACCOUNT_OPEN', 'DIGITAL_BANKING',
    'CASH_DEPOSIT', 'CASH_WITHDRAW', 'SAVINGS', 'INTERNATIONAL_TRANSFER',
    'CREDIT_CARD', 'IDENTITY_UPDATE', 'SIGNATURE_UPDATE',
  ];
  private servicesService = inject(ServicesService);
  private accountService = inject(AccountService);
  private monitorService = inject(QueueMonitorService);
  private ticketService = inject(TicketService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  services: Service[] = [];
  monitor: QueueMonitor | null = null;
  selectedService: Service | null = null;
  transactionForm: FormGroup = this.fb.group({});
  errorMessage = '';
  isLoading = true;
  isSubmitting = false;
  private profileValues: Record<string, string> = {};

  get fields(): FormFieldDefinition[] { return this.selectedService?.formSchema || []; }
  get sections(): string[] { return [...new Set(this.fields.map((field) => field.section || 'Thông tin giao dịch'))]; }
  fieldsInSection(section: string): FormFieldDefinition[] { return this.fields.filter((field) => (field.section || 'Thông tin giao dịch') === section); }
  serviceTypeLabel(type?: string): string {
    const labels: Record<string, string> = { CARD: 'Dịch vụ thẻ', ACCOUNT: 'Tài khoản', CASH: 'Tiền mặt', SAVINGS: 'Tiết kiệm', TRANSFER: 'Chuyển tiền', KYC: 'Cập nhật thông tin' };
    return labels[type || ''] || 'Dịch vụ tại quầy';
  }

  ngOnInit(): void {
    const branchId = Number(sessionStorage.getItem('selectedBranchId'));
    if (!branchId) { this.router.navigate(['/branches']); return; }
    this.servicesService.getMappedServicesByBranch(branchId).subscribe({
      next: (services) => { this.services = this.sortServices(services || []); this.isLoading = false; this.cdr.detectChanges(); },
      error: (error) => { this.errorMessage = this.apiError.getMessage(error, 'Không tải được danh sách dịch vụ.'); this.isLoading = false; this.cdr.detectChanges(); },
    });
    this.monitorService.getMonitor(branchId).subscribe({ next: (monitor) => { this.monitor = monitor; this.cdr.detectChanges(); } });
    this.accountService.getPaperlessProfile().subscribe({
      next: (profile) => {
        this.profileValues = profile.values || {};
        if (this.selectedService) this.applyProfileDefaults();
      },
    });
  }

  private sortServices(services: Service[]): Service[] {
    return [...services].sort((left, right) => {
      const leftIndex = this.serviceOrder.indexOf(left.serviceCode);
      const rightIndex = this.serviceOrder.indexOf(right.serviceCode);
      return (leftIndex < 0 ? Number.MAX_SAFE_INTEGER : leftIndex)
        - (rightIndex < 0 ? Number.MAX_SAFE_INTEGER : rightIndex)
        || left.serviceName.localeCompare(right.serviceName, 'vi');
    });
  }

  selectService(service: Service): void {
    const current = sessionStorage.getItem('currentTicket');
    if (current) {
      try { if (['WAITING', 'SERVING'].includes(JSON.parse(current)?.status)) { this.router.navigate(['/ticket']); return; } } catch { sessionStorage.removeItem('currentTicket'); }
    }
    this.selectedService = service;
    this.errorMessage = '';
    const controls: Record<string, unknown> = {};
    for (const field of service.formSchema || []) {
      const validators = field.required
        ? [field.type === 'CHECKBOX' ? Validators.requiredTrue : Validators.required]
        : [];
      controls[field.key] = [field.type === 'CHECKBOX' ? false : this.profileDefault(field.key), validators];
    }
    this.transactionForm = this.fb.group(controls);
    this.transactionForm.valueChanges.subscribe(() => { this.errorMessage = ''; });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  closeForm(): void { this.selectedService = null; this.transactionForm = this.fb.group({}); }

  submit(): void {
    if (!this.selectedService || this.transactionForm.invalid || this.isSubmitting) {
      this.transactionForm.markAllAsTouched();
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
    this.ticketService.createPreparedTicket(branchId, this.selectedService.serviceId, this.transactionForm.getRawValue()).subscribe({
      next: (ticket) => { sessionStorage.setItem('currentTicket', JSON.stringify(ticket)); this.router.navigate(['/ticket']); },
      error: (error) => { this.errorMessage = this.apiError.getMessage(error, 'Không thể tạo giao dịch nháp.'); this.isSubmitting = false; this.cdr.detectChanges(); },
    });
  }

  isInvalid(key: string): boolean { const control = this.transactionForm.get(key); return !!(control?.touched && control.invalid); }

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

  private profileDefault(fieldKey: string): string {
    const normalizedFieldKey = fieldKey.toLowerCase();
    const profileKeys: Record<string, string[]> = {
      fullname: ['FULL_NAME'],
      accountholder: ['FULL_NAME'],
      cardholdername: ['FULL_NAME'],
      phone: ['MOBILE_PHONE'],
      email: ['EMAIL_ADDRESS'],
      accountnumber: ['ACCOUNT_NUMBER'],
      debitaccount: ['ACCOUNT_NUMBER'],
      oldidentitynumber: ['IDENTITY_NUMBER'],
      identitynumber: ['IDENTITY_NUMBER'],
      address: ['CONTACT_ADDRESS', 'PERMANENT_ADDRESS'],
      employer: ['EMPLOYER_NAME'],
      monthlyincome: ['MONTHLY_INCOME'],
      deliveryaddress: ['CARD_DELIVERY_ADDRESS', 'CONTACT_ADDRESS'],
    };
    return (profileKeys[normalizedFieldKey] || [])
      .map((key) => this.profileValues[key])
      .find((value) => !!value?.trim()) || '';
  }
}
