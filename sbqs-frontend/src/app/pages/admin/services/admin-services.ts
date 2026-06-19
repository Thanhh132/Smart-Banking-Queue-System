import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { AppCard } from '../../../shared/components/app-card/app-card';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AdminServicesService } from '../../../core/services/admin-services.service';

@Component({
  selector: 'app-admin-services',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AppCard, DashboardLayout],
  templateUrl: './admin-services.html',
  styleUrl: './admin-services.scss',
})
export class AdminServices implements OnInit {
  private fb = inject(FormBuilder);
  private adminService = inject(AdminServicesService);
  private cdr = inject(ChangeDetectorRef);

  services: any[] = [];

  serviceForm!: FormGroup;

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
    const branchId = Number(localStorage.getItem('selectedBranchId')) || 1;

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
        console.error('Load services error:', err);
        this.errorMessage = 'Không thể tải danh sách dịch vụ.';
        this.isListLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  submitService(): void {
    if (this.isEditMode) {
      this.updateService();
      return;
    }

    this.createService();
  }

  createService(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.serviceForm.invalid) {
      this.serviceForm.markAllAsTouched();
      return;
    }

    const branchId = Number(localStorage.getItem('selectedBranchId')) || 1;

    const payload = {
      ...this.serviceForm.value,
      branch: {
        branchId,
      },
    };

    this.isSubmitting = true;

    this.adminService.createService(payload).subscribe({
      next: () => {
        this.successMessage = 'Tạo dịch vụ thành công.';
        this.serviceForm.reset({
          serviceCode: '',
          serviceName: '',
          serviceType: 'BASIC',
          description: '',
          estimatedTime: 10,
          status: 'ACTIVE',
        });
        this.isSubmitting = false;
        this.loadServices();
      },
      error: (err) => {
        console.error('Create service error:', err);
        this.errorMessage =
          err?.error?.message || err?.error || 'Tạo dịch vụ thất bại.';
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
      this.errorMessage = 'Không tìm thấy dịch vụ cần cập nhật.';
      return;
    }

    const branchId = Number(localStorage.getItem('selectedBranchId')) || 1;

    const payload = {
      ...this.serviceForm.value,
      branch: {
        branchId,
      },
    };

    this.isSubmitting = true;

    this.adminService.updateService(this.editingServiceId, payload).subscribe({
      next: () => {
        this.successMessage = 'Cập nhật dịch vụ thành công.';
        this.cancelEdit();
        this.isSubmitting = false;
        this.loadServices();
      },
      error: (err) => {
        console.error('Update service error:', err);
        this.errorMessage =
          err?.error?.message || err?.error || 'Cập nhật dịch vụ thất bại.';
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  deleteService(service: any): void {
    const confirmed = confirm(
      `Bạn có chắc muốn xóa dịch vụ "${service.serviceName}" không?`
    );

    if (!confirmed) {
      return;
    }``

    this.successMessage = '';
    this.errorMessage = '';

    this.adminService.deleteService(service.serviceId).subscribe({
      next: () => {
        this.successMessage = 'Xóa dịch vụ thành công.';
        this.loadServices();
      },
      error: (err) => {
        console.error('Delete service error:', err);
        this.errorMessage =
          err?.error?.message || err?.error || 'Xóa dịch vụ thất bại.';
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
      return `${label} không được để trống.`;
    }

    if (control.errors['min']) {
      return `${label} phải lớn hơn 0.`;
    }

    return `${label} không hợp lệ.`;
  }
}