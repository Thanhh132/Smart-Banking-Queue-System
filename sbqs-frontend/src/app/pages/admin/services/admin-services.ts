import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { forkJoin } from 'rxjs';

import { AppCard } from '../../../shared/components/app-card/app-card';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AdminServicesService } from '../../../core/services/admin-services.service';
import { ExcelImportPanel } from '../../../shared/components/excel-import-panel/excel-import-panel';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';

interface ServiceTemplate {
  key: string;
  name: string;
  type: string;
  estimatedTime: number;
  description: string;
  requiredCustomerFields?: string[];
}

interface PaperlessFieldOption {
  key: string;
  label: string;
}

@Component({
  selector: 'app-admin-services',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, AppCard, DashboardLayout, ExcelImportPanel, AppIcon],
  templateUrl: './admin-services.html',
  styleUrl: './admin-services.scss',
})
export class AdminServices implements OnInit {
  private fb = inject(FormBuilder);
  private adminService = inject(AdminServicesService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  services: any[] = [];
  serviceForm!: FormGroup;

  paperlessFieldOptions: PaperlessFieldOption[] = [
    { key: 'FULL_NAME', label: 'Họ và tên' },
    { key: 'DATE_OF_BIRTH', label: 'Ngày sinh' },
    { key: 'GENDER', label: 'Giới tính' },
    { key: 'NATIONALITY', label: 'Quốc tịch' },
    { key: 'IDENTITY_NUMBER', label: 'Số CCCD/CMND' },
    { key: 'IDENTITY_ISSUE_DATE', label: 'Ngày cấp CCCD' },
    { key: 'IDENTITY_ISSUE_PLACE', label: 'Nơi cấp CCCD' },
    { key: 'PASSPORT_NUMBER', label: 'Số hộ chiếu' },
    { key: 'VISA_NUMBER', label: 'Số thị thực' },
    { key: 'MOBILE_PHONE', label: 'Số điện thoại di động' },
    { key: 'EMAIL_ADDRESS', label: 'Địa chỉ email' },
    { key: 'PERMANENT_ADDRESS', label: 'Địa chỉ thường trú' },
    { key: 'CONTACT_ADDRESS', label: 'Địa chỉ cư trú hiện tại' },
    { key: 'OCCUPATION', label: 'Nghề nghiệp' },
    { key: 'EMPLOYMENT_STATUS', label: 'Tình trạng việc làm' },
    { key: 'EMPLOYER_NAME', label: 'Tên công ty/Cơ quan' },
    { key: 'WORK_PHONE', label: 'Số điện thoại nơi làm việc' },
    { key: 'JOB_TITLE', label: 'Chức vụ/Vị trí' },
    { key: 'MONTHLY_INCOME', label: 'Thu nhập trung bình hàng tháng' },
    { key: 'SALARY_PAYMENT_METHOD', label: 'Hình thức nhận lương' },
    { key: 'ACCOUNT_NUMBER', label: 'Số tài khoản liên kết' },
    { key: 'CARD_DELIVERY_ADDRESS', label: 'Địa chỉ nhận thẻ' },
  ];

  serviceCatalog: ServiceTemplate[] = [
    {
      key: 'CASH_WITHDRAW',
      name: 'Rút tiền',
      type: 'BASIC',
      estimatedTime: 7,
      description: 'Rút tiền mặt tại quầy',
    },
    {
      key: 'CASH_DEPOSIT',
      name: 'Nộp tiền',
      type: 'BASIC',
      estimatedTime: 8,
      description: 'Nộp tiền vào tài khoản',
    },
    {
      key: 'TRANSFER',
      name: 'Chuyển khoản',
      type: 'BASIC',
      estimatedTime: 10,
      description: 'Hỗ trợ chuyển khoản tại quầy',
    },
    {
      key: 'ACCOUNT_OPEN',
      name: 'Mở tài khoản',
      type: 'BASIC',
      estimatedTime: 15,
      description: 'Đăng ký tài khoản mới',
    },
    {
      key: 'CREDIT_CARD_REGISTER',
      name: 'Đăng ký mở thẻ tín dụng',
      type: 'CARD',
      estimatedTime: 25,
      description: 'Đăng ký phát hành thẻ tín dụng và thu thập thông tin thẩm định cơ bản',
      requiredCustomerFields: [
        'FULL_NAME',
        'DATE_OF_BIRTH',
        'GENDER',
        'NATIONALITY',
        'IDENTITY_NUMBER',
        'IDENTITY_ISSUE_DATE',
        'IDENTITY_ISSUE_PLACE',
        'MOBILE_PHONE',
        'EMAIL_ADDRESS',
        'PERMANENT_ADDRESS',
        'CONTACT_ADDRESS',
        'OCCUPATION',
        'EMPLOYMENT_STATUS',
        'EMPLOYER_NAME',
        'WORK_PHONE',
        'JOB_TITLE',
        'MONTHLY_INCOME',
        'SALARY_PAYMENT_METHOD',
        'CARD_DELIVERY_ADDRESS',
      ],
    },
    {
      key: 'PHYSICAL_CARD_LINKED_ACCOUNT',
      name: 'Đăng ký mở thẻ vật lý liên kết tài khoản',
      type: 'CARD',
      estimatedTime: 18,
      description: 'Phát hành thẻ vật lý gắn với tài khoản thanh toán hiện có',
      requiredCustomerFields: [
        'FULL_NAME',
        'DATE_OF_BIRTH',
        'GENDER',
        'NATIONALITY',
        'IDENTITY_NUMBER',
        'IDENTITY_ISSUE_DATE',
        'IDENTITY_ISSUE_PLACE',
        'MOBILE_PHONE',
        'EMAIL_ADDRESS',
        'PERMANENT_ADDRESS',
        'CONTACT_ADDRESS',
        'ACCOUNT_NUMBER',
        'CARD_DELIVERY_ADDRESS',
      ],
    },
    {
      key: 'CARD_REISSUE',
      name: 'Cấp lại thẻ',
      type: 'CARD',
      estimatedTime: 12,
      description: 'Cấp lại thẻ mất hoặc hỏng',
      requiredCustomerFields: [
        'FULL_NAME',
        'IDENTITY_NUMBER',
        'IDENTITY_ISSUE_DATE',
        'IDENTITY_ISSUE_PLACE',
        'MOBILE_PHONE',
        'EMAIL_ADDRESS',
        'CONTACT_ADDRESS',
        'ACCOUNT_NUMBER',
        'CARD_DELIVERY_ADDRESS',
      ],
    },
    {
      key: 'CARD_REREGISTER',
      name: 'Đăng ký lại thẻ',
      type: 'CARD',
      estimatedTime: 15,
      description: 'Đăng ký lại thẻ khi hết hạn, thất lạc hoặc thay đổi thông tin phát hành',
      requiredCustomerFields: [
        'FULL_NAME',
        'IDENTITY_NUMBER',
        'IDENTITY_ISSUE_DATE',
        'IDENTITY_ISSUE_PLACE',
        'MOBILE_PHONE',
        'EMAIL_ADDRESS',
        'CONTACT_ADDRESS',
        'ACCOUNT_NUMBER',
        'CARD_DELIVERY_ADDRESS',
      ],
    },
    {
      key: 'CARD_PIN',
      name: 'Đổi PIN hoặc mở khóa thẻ',
      type: 'CARD',
      estimatedTime: 8,
      description: 'Hỗ trợ PIN và trạng thái thẻ',
    },
    {
      key: 'LOAN_CONSULT',
      name: 'Tư vấn vay',
      type: 'LOAN',
      estimatedTime: 20,
      description: 'Tư vấn sản phẩm tín dụng',
    },
    {
      key: 'LOAN_PAYMENT',
      name: 'Thanh toán khoản vay',
      type: 'LOAN',
      estimatedTime: 12,
      description: 'Hỗ trợ nộp tiền thanh toán khoản vay',
    },
    {
      key: 'CUSTOMER_SUPPORT',
      name: 'Hỗ trợ khách hàng',
      type: 'SUPPORT',
      estimatedTime: 10,
      description: 'Giải đáp và xử lý yêu cầu chung',
    },
    {
      key: 'INFORMATION_UPDATE',
      name: 'Cập nhật thông tin',
      type: 'SUPPORT',
      estimatedTime: 12,
      description: 'Cập nhật thông tin cá nhân hoặc KYC',
    },
    {
      key: 'COMPLAINT',
      name: 'Khiếu nại tra soát',
      type: 'SUPPORT',
      estimatedTime: 18,
      description: 'Tiếp nhận khiếu nại và tra soát giao dịch',
    },
  ];

  selectedTemplateKeys: string[] = [];
  searchTerm = '';
  isListLoading = false;
  isSubmitting = false;
  isEditMode = false;
  editingServiceId: number | null = null;
  successMessage = '';
  errorMessage = '';

  get activeServiceCount(): number {
    return this.services.filter((service) => service.status === 'ACTIVE').length;
  }

  get inactiveServiceCount(): number {
    return this.services.length - this.activeServiceCount;
  }

  get filteredServices(): any[] {
    const keyword = this.searchTerm.trim().toLocaleLowerCase('vi');
    if (!keyword) {
      return this.services;
    }

    return this.services.filter((service) =>
      [service.serviceCode, service.serviceName, service.serviceType, service.description]
        .filter(Boolean)
        .some((value) => String(value).toLocaleLowerCase('vi').includes(keyword))
    );
  }

  ngOnInit(): void {
    this.initForm();
    this.loadServices();
  }

  initForm(): void {
    this.serviceForm = this.fb.group({
      serviceCode: ['', [Validators.required]],
      serviceName: ['', [Validators.required]],
      serviceType: ['BASIC', [Validators.required]],
      description: [''],
      estimatedTime: [10, [Validators.required, Validators.min(1)]],
      status: ['ACTIVE', [Validators.required]],
      requiredCustomerFields: [[]],
    });
  }

  loadServices(): void {
    const branchId = this.getBranchId();

    if (!branchId) {
      return;
    }

    this.isListLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.adminService.getServicesByBranch(branchId).subscribe({
      next: (data) => {
        this.services = Array.isArray(data) ? data : [];
        this.isListLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không tải được danh sách dịch vụ.');
        this.isListLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  toggleTemplate(key: string, checked: boolean): void {
    if (checked) {
      this.selectedTemplateKeys = Array.from(new Set([...this.selectedTemplateKeys, key]));
      return;
    }

    this.selectedTemplateKeys = this.selectedTemplateKeys.filter((item) => item !== key);
  }

  createSelectedServices(): void {
    const branchId = this.getBranchId();

    if (!branchId) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    const templates = this.serviceCatalog.filter((item) =>
      this.selectedTemplateKeys.includes(item.key)
    );

    if (templates.length === 0) {
      this.errorMessage = 'Hãy chọn ít nhất một dịch vụ mẫu.';
      this.cdr.detectChanges();
      return;
    }

    const existingNames = new Set(
      this.services.map((service) => String(service.serviceName).toLowerCase())
    );

    const filteredTemplates = templates.filter(
      (template) => !existingNames.has(template.name.toLowerCase())
    );

    if (filteredTemplates.length === 0) {
      this.errorMessage = 'Các dịch vụ đã chọn đã tồn tại trong chi nhánh.';
      this.cdr.detectChanges();
      return;
    }

    const startNumber = this.services.length + 1;
    const requests = filteredTemplates.map((template, index) => {
      const payload = {
        serviceCode: this.generateServiceCode(branchId, template.type, startNumber + index),
        serviceName: template.name,
        serviceType: template.type,
        description: template.description,
        estimatedTime: template.estimatedTime,
        status: 'ACTIVE',
        requiredCustomerFields: template.requiredCustomerFields || [],
        branch: { branchId },
      };

      return this.adminService.createService(payload);
    });

    this.isSubmitting = true;

    forkJoin(requests).subscribe({
      next: () => {
        this.successMessage = `Đã tạo ${filteredTemplates.length} dịch vụ.`;
        this.selectedTemplateKeys = [];
        this.isSubmitting = false;
        this.loadServices();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Tạo dịch vụ hàng loạt thất bại.');
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  startEdit(service: any): void {
    this.isEditMode = true;
    this.editingServiceId = service.serviceId;

    this.serviceForm.patchValue({
      serviceCode: service.serviceCode,
      serviceName: service.serviceName,
      serviceType: service.serviceType || 'BASIC',
      description: service.description || '',
      estimatedTime: service.estimatedTime,
      status: service.status || 'ACTIVE',
      requiredCustomerFields: service.requiredCustomerFields || [],
    });

    this.cdr.detectChanges();
  }

  updateService(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.serviceForm.invalid) {
      this.serviceForm.markAllAsTouched();
      return;
    }

    if (!this.editingServiceId) {
      this.errorMessage = 'Không tìm thấy dịch vụ cần cập nhật.';
      return;
    }

    const branchId = this.getBranchId();

    if (!branchId) {
      return;
    }

    const payload = {
      ...this.serviceForm.value,
      branch: { branchId },
    };

    this.isSubmitting = true;

    this.adminService.updateService(this.editingServiceId, payload).subscribe({
      next: () => {
        this.successMessage = 'Đã cập nhật dịch vụ.';
        this.cancelEdit();
        this.isSubmitting = false;
        this.loadServices();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Cập nhật dịch vụ thất bại.');
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  deleteService(service: any): void {
    if (!confirm(`Xóa dịch vụ "${service.serviceName}"?`)) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.adminService.deleteService(service.serviceId).subscribe({
      next: () => {
        this.successMessage = 'Đã xóa dịch vụ.';
        this.loadServices();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Xóa dịch vụ thất bại.');
        this.cdr.detectChanges();
      },
    });
  }

  cancelEdit(): void {
    this.isEditMode = false;
    this.editingServiceId = null;

    this.serviceForm.reset({
      serviceCode: '',
      serviceName: '',
      serviceType: 'BASIC',
      description: '',
      estimatedTime: 10,
      status: 'ACTIVE',
      requiredCustomerFields: [],
    });

    this.cdr.detectChanges();
  }

  isInvalid(controlName: string): boolean {
    const control = this.serviceForm.get(controlName);
    return !!(control && control.touched && control.invalid);
  }

  getErrorMessage(controlName: string, label: string): string {
    const control = this.serviceForm.get(controlName);

    if (!control || !control.errors) {
      return '';
    }

    if (control.errors['required']) {
      return `${label} không được để trống.`;
    }

    if (control.errors['min']) {
      return `${label} phải lớn hơn 0.`;
    }

    return `${label} không hợp lệ.`;
  }

  toggleRequiredField(key: string, checked: boolean): void {
    const control = this.serviceForm.get('requiredCustomerFields');
    const current = (control?.value || []) as string[];
    const next = checked
      ? Array.from(new Set([...current, key]))
      : current.filter((item) => item !== key);
    control?.setValue(next);
    control?.markAsDirty();
  }

  hasRequiredField(key: string): boolean {
    const current = (this.serviceForm.get('requiredCustomerFields')?.value || []) as string[];
    return current.includes(key);
  }

  getRequiredFieldLabel(key: string): string {
    return this.paperlessFieldOptions.find((field) => field.key === key)?.label || key;
  }

  private generateServiceCode(branchId: number, type: string, number: number): string {
    return `S-${branchId}-${type}-${number}`;
  }

  private getBranchId(): number | null {
    const branchId = Number(localStorage.getItem('selectedBranchId'));

    if (!branchId) {
      this.errorMessage =
        'Tài khoản Branch Admin này chưa được gán chi nhánh. Hãy dùng tài khoản do Super Admin cấp cho chi nhánh.';
      this.isListLoading = false;
      this.isSubmitting = false;
      this.cdr.detectChanges();
      return null;
    }

    return branchId;
  }
}
