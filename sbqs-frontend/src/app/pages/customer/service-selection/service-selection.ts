import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { QueueMonitor } from '../../../core/models/queue-monitor.model';
import { Service } from '../../../core/models/service.model';
import { Branch } from '../../../core/models/branch.model';
import { AccountService, CustomerProfileField } from '../../../core/services/account.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { BranchService } from '../../../core/services/branch.service';
import { QueueMonitorService } from '../../../core/services/queue-monitor.service';
import { ServicesService } from '../../../core/services/services.service';
import { TicketService } from '../../../core/services/ticket.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';

@Component({
  selector: 'app-service-selection',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DashboardLayout, AppIcon],
  templateUrl: './service-selection.html',
  styleUrl: './service-selection.scss',
})
export class ServiceSelection implements OnInit {
  private servicesService = inject(ServicesService);
  private monitorService = inject(QueueMonitorService);
  private ticketService = inject(TicketService);
  private accountService = inject(AccountService);
  private branchService = inject(BranchService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  services: Service[] = [];
  monitor: QueueMonitor | null = null;
  selectedBranch: Branch | null = null;
  selectedService: Service | null = null;
  requiredProfileFields: CustomerProfileField[] = [];
  profileForm: FormGroup = this.fb.group({});
  cardDeliveryMethod: 'BRANCH' | 'DELIVERY' = 'BRANCH';
  errorMessage = '';
  isLoading = false;
  isCreatingTicket = false;
  isLoadingProfile = false;
  isSavingProfile = false;
  showPaperlessForm = false;

  get servingCounterCount(): number {
    return this.monitor?.servingCounters?.filter((counter) => counter.status === 'SERVING').length || 0;
  }

  getCounterStatusLabel(status: string): string {
    if (status === 'SERVING') {
      return 'Đang phục vụ';
    }

    if (status === 'IDLE') {
      return 'Đang rảnh';
    }

    return 'Không hoạt động';
  }

  getCounterStatusClass(status: string): string {
    return `customer-counter--${status.toLowerCase()}`;
  }

  ngOnInit(): void {
    this.loadPage();
    this.syncCurrentTicket();
  }

  loadPage(): void {
    const branchId = this.getBranchId();

    if (!branchId) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.servicesService.getMappedServicesByBranch(branchId).subscribe({
      next: (data) => {
        this.services = this.normalizeServiceList(data || []);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không tải được danh sách dịch vụ.'
        );
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });

    this.monitorService.getMonitor(branchId).subscribe({
      next: (data) => {
        this.monitor = data;
        this.cdr.detectChanges();
      },
      error: () => {
        this.monitor = null;
        this.cdr.detectChanges();
      },
    });

    this.loadSelectedBranch(branchId);
  }

  selectService(service: Service): void {
    const branchId = this.getBranchId();

    if (!branchId) {
      return;
    }

    const currentTicket = this.getCurrentActiveTicket();
    if (currentTicket) {
      this.errorMessage =
        'Bạn đang có một phiếu chưa hoàn thành. Hãy theo dõi hoặc hủy phiếu hiện tại trước.';
      this.router.navigate(['/ticket']);
      return;
    }

    this.selectedService = service;
    this.isLoadingProfile = true;
    this.errorMessage = '';

    this.accountService.getPaperlessProfile(service.serviceId).subscribe({
      next: (profile) => {
        this.isLoadingProfile = false;
        if (profile.complete || profile.requiredFields.length === 0) {
          this.issueTicket(branchId, service);
          return;
        }

        this.requiredProfileFields = profile.requiredFields;
        this.buildProfileForm(profile.values || {});
        this.showPaperlessForm = true;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không kiểm tra được hồ sơ giấy tờ. Vui lòng thử lại.'
        );
        this.isLoadingProfile = false;
        this.cdr.detectChanges();
      },
    });
  }

  submitPaperlessProfile(): void {
    if (!this.selectedService) {
      return;
    }

    const branchId = this.getBranchId();
    if (!branchId) {
      return;
    }

    this.syncCardDeliveryAddress();

    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSavingProfile = true;
    this.errorMessage = '';

    this.accountService.updatePaperlessProfile({
      serviceId: this.selectedService.serviceId,
      values: this.profileForm.value,
    }).subscribe({
      next: () => {
        this.isSavingProfile = false;
        this.showPaperlessForm = false;
        this.issueTicket(branchId, this.selectedService!);
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không lưu được hồ sơ giấy tờ. Vui lòng kiểm tra lại thông tin.'
        );
        this.isSavingProfile = false;
        this.cdr.detectChanges();
      },
    });
  }

  cancelPaperlessForm(): void {
    this.showPaperlessForm = false;
    this.selectedService = null;
    this.requiredProfileFields = [];
    this.profileForm = this.fb.group({});
  }

  isProfileFieldInvalid(key: string): boolean {
    const control = this.profileForm.get(key);
    return !!(control && control.touched && control.invalid);
  }

  getProfileInputType(field: CustomerProfileField): string {
    return field.type === 'date' ? 'date' : field.type;
  }

  getProfilePlaceholder(field: CustomerProfileField): string {
    return field.placeholder;
  }

  isSelectProfileField(field: CustomerProfileField): boolean {
    return field.key === 'GENDER' || field.key === 'EMPLOYMENT_STATUS' || field.key === 'SALARY_PAYMENT_METHOD';
  }

  getProfileOptions(field: CustomerProfileField): string[] {
    if (field.key === 'GENDER') {
      return ['Nam', 'Nữ', 'Khác'];
    }
    if (field.key === 'EMPLOYMENT_STATUS') {
      return ['Nhân viên hợp đồng', 'Chủ doanh nghiệp', 'Tự do', 'Học sinh - Sinh viên', 'Khác'];
    }
    if (field.key === 'SALARY_PAYMENT_METHOD') {
      return ['Chuyển khoản', 'Tiền mặt', 'Khác'];
    }
    return [];
  }

  isCardDeliveryAddressField(field: CustomerProfileField): boolean {
    return field.key === 'CARD_DELIVERY_ADDRESS';
  }

  onCardDeliveryMethodChange(method: 'BRANCH' | 'DELIVERY'): void {
    this.cardDeliveryMethod = method;
    this.syncCardDeliveryAddress(true);
  }

  getBranchPickupAddress(): string {
    if (this.selectedBranch) {
      return this.formatBranchAddress(this.selectedBranch);
    }
    return this.monitor?.branchName || 'Chi nhánh đã chọn';
  }

  getProfileGroupLabel(key: string): string {
    if (['FULL_NAME', 'DATE_OF_BIRTH', 'GENDER', 'NATIONALITY', 'IDENTITY_NUMBER', 'IDENTITY_ISSUE_DATE', 'IDENTITY_ISSUE_PLACE', 'PASSPORT_NUMBER', 'VISA_NUMBER'].includes(key)) {
      return 'Thông tin cá nhân và định danh';
    }
    if (['MOBILE_PHONE', 'EMAIL_ADDRESS', 'PERMANENT_ADDRESS', 'CONTACT_ADDRESS'].includes(key)) {
      return 'Thông tin liên lạc và địa chỉ';
    }
    if (['OCCUPATION', 'EMPLOYMENT_STATUS', 'EMPLOYER_NAME', 'WORK_PHONE', 'JOB_TITLE', 'MONTHLY_INCOME', 'SALARY_PAYMENT_METHOD'].includes(key)) {
      return 'Thông tin nghề nghiệp và tài chính';
    }
    return 'Thông tin tài khoản và nhận thẻ';
  }

  isFirstProfileFieldInGroup(index: number): boolean {
    const current = this.requiredProfileFields[index];
    const previous = this.requiredProfileFields[index - 1];
    return !previous || this.getProfileGroupLabel(previous.key) !== this.getProfileGroupLabel(current.key);
  }

  getProfileErrorMessage(key: string): string {
    const control = this.profileForm.get(key);
    if (control?.hasError('pattern')) {
      if (key === 'IDENTITY_NUMBER') {
        return 'Số CCCD/CMND phải gồm 12 chữ số.';
      }
      if (['MOBILE_PHONE', 'WORK_PHONE'].includes(key)) {
        return 'Số điện thoại chưa đúng định dạng Việt Nam.';
      }
    }
    if (control?.hasError('email')) {
      return 'Email chưa đúng định dạng.';
    }
    return 'Thông tin này bắt buộc.';
  }

  private getBranchId(): number | null {
    const branchId = Number(localStorage.getItem('selectedBranchId'));

    if (!branchId) {
      this.errorMessage = 'Bạn chưa chọn chi nhánh.';
      this.cdr.detectChanges();
      return null;
    }

    return branchId;
  }

  private getCurrentActiveTicket(): any | null {
    const rawTicket = localStorage.getItem('currentTicket');

    if (!rawTicket) {
      return null;
    }

    try {
      const ticket = JSON.parse(rawTicket);
      return ['WAITING', 'SERVING'].includes(ticket?.status) ? ticket : null;
    } catch {
      localStorage.removeItem('currentTicket');
      return null;
    }
  }

  private buildProfileForm(values: Record<string, string>): void {
    const controls: Record<string, any> = {};
    this.requiredProfileFields.forEach((field) => {
      const validators: ValidatorFn[] = field.required ? [Validators.required] : [];
      if (field.key === 'IDENTITY_NUMBER') {
        validators.push(Validators.pattern(/^\d{12}$/));
      }
      if (['MOBILE_PHONE', 'WORK_PHONE'].includes(field.key)) {
        validators.push(Validators.pattern(/^0\d{9,10}$/));
      }
      if (field.key === 'EMAIL_ADDRESS') {
        validators.push(Validators.email);
      }
      controls[field.key] = [this.getInitialProfileValue(field, values[field.key]), validators];
    });
    this.profileForm = this.fb.group(controls);
    this.cardDeliveryMethod = values['CARD_DELIVERY_ADDRESS']?.trim() ? 'DELIVERY' : 'BRANCH';
    this.applyAddressDefaults();
    this.profileForm.get('PERMANENT_ADDRESS')?.valueChanges.subscribe(() => this.applyAddressDefaults());
    this.profileForm.get('CONTACT_ADDRESS')?.valueChanges.subscribe(() => this.applyAddressDefaults());
  }

  private getInitialProfileValue(field: CustomerProfileField, value?: string): string {
    if (value) {
      return field.type === 'date' ? this.toDateInputValue(value) : value;
    }
    return field.key === 'NATIONALITY' ? 'Việt Nam' : '';
  }

  private toDateInputValue(value: string): string {
    const trimmed = value.trim();
    const vietnameseDate = trimmed.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
    if (vietnameseDate) {
      return `${vietnameseDate[3]}-${vietnameseDate[2]}-${vietnameseDate[1]}`;
    }
    return trimmed;
  }

  private applyAddressDefaults(): void {
    const permanentAddress = this.profileForm.get('PERMANENT_ADDRESS')?.value?.trim();
    const contactAddressControl = this.profileForm.get('CONTACT_ADDRESS');

    if (permanentAddress && contactAddressControl && !contactAddressControl.value?.trim()) {
      contactAddressControl.setValue(permanentAddress, { emitEvent: false });
    }

    this.syncCardDeliveryAddress();
  }

  private syncCardDeliveryAddress(force = false): void {
    const cardAddressControl = this.profileForm.get('CARD_DELIVERY_ADDRESS');
    if (!cardAddressControl) {
      return;
    }

    const branchAddress = this.getBranchPickupAddress();
    const permanentAddress = this.profileForm.get('PERMANENT_ADDRESS')?.value?.trim();
    const contactAddress = this.profileForm.get('CONTACT_ADDRESS')?.value?.trim() || permanentAddress || '';
    const currentAddress = cardAddressControl.value?.trim() || '';

    if (this.cardDeliveryMethod === 'BRANCH') {
      cardAddressControl.setValue(branchAddress, { emitEvent: false });
      return;
    }

    if (force || !currentAddress || currentAddress === branchAddress) {
      cardAddressControl.setValue(contactAddress, { emitEvent: false });
    }
  }

  private loadSelectedBranch(branchId: number): void {
    this.branchService.getBranches().subscribe({
      next: (branches) => {
        this.selectedBranch = (branches || []).find((branch) => branch.branchId === branchId) || null;
        this.syncCardDeliveryAddress();
        this.cdr.detectChanges();
      },
      error: () => {
        this.selectedBranch = null;
        this.syncCardDeliveryAddress();
        this.cdr.detectChanges();
      },
    });
  }

  private formatBranchAddress(branch: Branch): string {
    return [
      branch.bankName,
      branch.branchName,
      branch.address,
      branch.ward,
      branch.district,
      branch.province,
    ]
      .filter(Boolean)
      .join(', ');
  }

  private issueTicket(branchId: number, service: Service): void {
    this.isCreatingTicket = true;
    this.errorMessage = '';

    this.ticketService.createTicket(branchId, service.serviceId).subscribe({
      next: (ticket: any) => {
        localStorage.setItem('currentTicket', JSON.stringify(ticket));
        this.isCreatingTicket = false;
        this.router.navigate(['/ticket']);
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không tạo được phiếu. Vui lòng thử lại.'
        );
        this.isCreatingTicket = false;
        this.cdr.detectChanges();
      },
    });
  }

  private syncCurrentTicket(): void {
    this.ticketService.getCurrentTicket().subscribe({
      next: (ticket: any) => {
        if (ticket) {
          localStorage.setItem('currentTicket', JSON.stringify(ticket));
        } else {
          localStorage.removeItem('currentTicket');
        }
      },
      error: () => {
        localStorage.removeItem('currentTicket');
      },
    });
  }

  private normalizeServiceList(services: Service[]): Service[] {
    const normalized = services.map((service) => this.normalizeServiceDisplay(service));
    const hasSpecificCardRegistration = normalized.some((service) =>
      ['CREDIT_CARD_REGISTER', 'PHYSICAL_CARD_LINKED_ACCOUNT'].includes(this.getServiceIdentity(service))
    );
    const seen = new Set<string>();

    return normalized.filter((service) => {
      const identity = this.getServiceIdentity(service);
      if (hasSpecificCardRegistration && identity === 'CARD_REGISTER') {
        return false;
      }
      if (seen.has(identity)) {
        return false;
      }
      seen.add(identity);
      return true;
    });
  }

  private normalizeServiceDisplay(service: Service): Service {
    const identity = this.getServiceIdentity(service);
    const catalog: Record<string, Partial<Service>> = {
      CASH_DEPOSIT: {
        serviceName: 'Nộp tiền',
        description: 'Nộp tiền vào tài khoản',
      },
      CASH_WITHDRAW: {
        serviceName: 'Rút tiền',
        description: 'Rút tiền mặt tại quầy',
      },
      TRANSFER: {
        serviceName: 'Chuyển khoản',
        description: 'Hỗ trợ chuyển khoản tại quầy',
      },
      ACCOUNT_OPEN: {
        serviceName: 'Mở tài khoản',
        description: 'Đăng ký tài khoản thanh toán mới',
      },
      CARD_REGISTER: {
        serviceName: 'Đăng ký mở thẻ',
        description: 'Đăng ký phát hành thẻ ngân hàng',
      },
      CREDIT_CARD_REGISTER: {
        serviceName: 'Đăng ký mở thẻ tín dụng',
        description: 'Phát hành thẻ tín dụng và thu thập thông tin thẩm định cơ bản',
      },
      PHYSICAL_CARD_LINKED_ACCOUNT: {
        serviceName: 'Đăng ký mở thẻ vật lý liên kết tài khoản',
        description: 'Phát hành thẻ vật lý gắn với tài khoản thanh toán hiện có',
      },
      CARD_REISSUE: {
        serviceName: 'Cấp lại thẻ',
        description: 'Cấp lại thẻ bị mất, hỏng hoặc cần thay thế',
      },
      CARD_REREGISTER: {
        serviceName: 'Đăng ký lại thẻ',
        description: 'Đăng ký lại thẻ khi hết hạn hoặc thay đổi thông tin phát hành',
      },
      CARD_PIN: {
        serviceName: 'Đổi PIN hoặc mở khóa thẻ',
        description: 'Hỗ trợ PIN và trạng thái thẻ',
      },
      LOAN_CONSULT: {
        serviceName: 'Tư vấn vay',
        description: 'Tư vấn sản phẩm tín dụng',
      },
      LOAN_PAYMENT: {
        serviceName: 'Thanh toán khoản vay',
        description: 'Thanh toán khoản vay tại quầy',
      },
    };

    return {
      ...service,
      ...catalog[identity],
    };
  }

  private getServiceIdentity(service: Service): string {
    const text = this.normalizeText(`${service.serviceCode || ''} ${service.serviceName || ''}`);
    if (text.includes('credit') || text.includes('tin dung')) {
      return 'CREDIT_CARD_REGISTER';
    }
    if (text.includes('physical') || text.includes('vat ly') || text.includes('lien ket tai khoan')) {
      return 'PHYSICAL_CARD_LINKED_ACCOUNT';
    }
    if (text.includes('reregister') || text.includes('dang ky lai the')) {
      return 'CARD_REREGISTER';
    }
    if (text.includes('reissue') || text.includes('cap lai the')) {
      return 'CARD_REISSUE';
    }
    if (text.includes('card register') || text.includes('dang ky the') || text.includes('dang ky mo the')) {
      return 'CARD_REGISTER';
    }
    if (text.includes('pin')) {
      return 'CARD_PIN';
    }
    if (text.includes('withdraw') || text.includes('rut tien')) {
      return 'CASH_WITHDRAW';
    }
    if (text.includes('deposit') || text.includes('nop tien')) {
      return 'CASH_DEPOSIT';
    }
    if (text.includes('transfer') || text.includes('chuyen khoan')) {
      return 'TRANSFER';
    }
    if (text.includes('account_open') || text.includes('mo tai khoan')) {
      return 'ACCOUNT_OPEN';
    }
    if (text.includes('loan_payment') || text.includes('thanh toan khoan vay')) {
      return 'LOAN_PAYMENT';
    }
    if (text.includes('loan') || text.includes('vay')) {
      return 'LOAN_CONSULT';
    }
    return text || String(service.serviceId);
  }

  private normalizeText(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/đ/g, 'd')
      .replace(/[^a-z0-9]+/g, ' ')
      .trim();
  }
}
