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

interface ServiceTemplate {
  key: string;
  name: string;
  type: string;
  estimatedTime: number;
  description: string;
}

@Component({
  selector: 'app-admin-services',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, AppCard, DashboardLayout],
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

  serviceCatalog: ServiceTemplate[] = [
    {
      key: 'CASH_WITHDRAW',
      name: 'Rut tien',
      type: 'BASIC',
      estimatedTime: 7,
      description: 'Rut tien mat tai quay',
    },
    {
      key: 'CASH_DEPOSIT',
      name: 'Nop tien',
      type: 'BASIC',
      estimatedTime: 8,
      description: 'Nop tien vao tai khoan',
    },
    {
      key: 'TRANSFER',
      name: 'Chuyen khoan',
      type: 'BASIC',
      estimatedTime: 10,
      description: 'Ho tro chuyen khoan tai quay',
    },
    {
      key: 'ACCOUNT_OPEN',
      name: 'Mo tai khoan',
      type: 'BASIC',
      estimatedTime: 15,
      description: 'Dang ky tai khoan moi',
    },
    {
      key: 'CARD_REGISTER',
      name: 'Dang ky the',
      type: 'CARD',
      estimatedTime: 15,
      description: 'Dang ky the ATM/ghi no/tin dung',
    },
    {
      key: 'CARD_REISSUE',
      name: 'Cap lai the',
      type: 'CARD',
      estimatedTime: 12,
      description: 'Cap lai the mat/hong',
    },
    {
      key: 'CARD_PIN',
      name: 'Doi PIN hoac mo khoa the',
      type: 'CARD',
      estimatedTime: 8,
      description: 'Ho tro PIN va trang thai the',
    },
    {
      key: 'LOAN_CONSULT',
      name: 'Tu van vay',
      type: 'LOAN',
      estimatedTime: 20,
      description: 'Tu van san pham tin dung',
    },
    {
      key: 'LOAN_PAYMENT',
      name: 'Thanh toan khoan vay',
      type: 'LOAN',
      estimatedTime: 12,
      description: 'Ho tro nop tien thanh toan khoan vay',
    },
    {
      key: 'CUSTOMER_SUPPORT',
      name: 'Ho tro khach hang',
      type: 'SUPPORT',
      estimatedTime: 10,
      description: 'Giai dap va xu ly yeu cau chung',
    },
    {
      key: 'INFORMATION_UPDATE',
      name: 'Cap nhat thong tin',
      type: 'SUPPORT',
      estimatedTime: 12,
      description: 'Cap nhat thong tin ca nhan/KYC',
    },
    {
      key: 'COMPLAINT',
      name: 'Khieu nai tra soat',
      type: 'SUPPORT',
      estimatedTime: 18,
      description: 'Tiep nhan khieu nai va tra soat giao dich',
    },
  ];

  selectedTemplateKeys: string[] = [];
  isListLoading = false;
  isSubmitting = false;
  isEditMode = false;
  editingServiceId: number | null = null;
  successMessage = '';
  errorMessage = '';

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
        this.errorMessage = this.apiError.getMessage(err, 'Khong tai duoc danh sach dich vu.');
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
      this.errorMessage = 'Hay chon it nhat mot dich vu mau.';
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
      this.errorMessage = 'Cac dich vu da chon da ton tai trong chi nhanh.';
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
        branch: { branchId },
      };

      return this.adminService.createService(payload);
    });

    this.isSubmitting = true;

    forkJoin(requests).subscribe({
      next: () => {
        this.successMessage = `Da tao ${filteredTemplates.length} dich vu.`;
        this.selectedTemplateKeys = [];
        this.isSubmitting = false;
        this.loadServices();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Tao dich vu hang loat that bai.');
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
      this.errorMessage = 'Khong tim thay dich vu can cap nhat.';
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
        this.successMessage = 'Da cap nhat dich vu.';
        this.cancelEdit();
        this.isSubmitting = false;
        this.loadServices();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Cap nhat dich vu that bai.');
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  deleteService(service: any): void {
    if (!confirm(`Xoa dich vu "${service.serviceName}"?`)) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.adminService.deleteService(service.serviceId).subscribe({
      next: () => {
        this.successMessage = 'Da xoa dich vu.';
        this.loadServices();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Xoa dich vu that bai.');
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
      return `${label} khong duoc de trong.`;
    }

    if (control.errors['min']) {
      return `${label} phai lon hon 0.`;
    }

    return `${label} khong hop le.`;
  }

  private generateServiceCode(branchId: number, type: string, number: number): string {
    return `S-${branchId}-${type}-${number}`;
  }

  private getBranchId(): number | null {
    const branchId = Number(localStorage.getItem('selectedBranchId'));

    if (!branchId) {
      this.errorMessage =
        'Tai khoan Branch Admin nay chua duoc gan chi nhanh. Hay dung tai khoan do Super Admin cap cho chi nhanh.';
      this.isListLoading = false;
      this.isSubmitting = false;
      this.cdr.detectChanges();
      return null;
    }

    return branchId;
  }
}
