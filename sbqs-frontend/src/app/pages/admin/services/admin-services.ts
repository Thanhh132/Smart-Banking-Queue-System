import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { FormFieldDefinition, FormFieldType, Service } from '../../../core/models/service.model';
import { AdminServicesService } from '../../../core/services/admin-services.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AppButton } from '../../../shared/components/app-button/app-button';
import { AppCard } from '../../../shared/components/app-card/app-card';
import { AppEmptyState } from '../../../shared/components/app-empty-state/app-empty-state';
import { AppLoadingState } from '../../../shared/components/app-loading-state/app-loading-state';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { AppStatusBadge } from '../../../shared/components/app-status-badge/app-status-badge';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({
  selector: 'app-admin-services',
  standalone: true,
  imports: [CommonModule, FormsModule, DashboardLayout, AppButton, AppCard, AppEmptyState, AppLoadingState, AppPageHeader, AppStatusBadge],
  templateUrl: './admin-services.html',
  styleUrl: './admin-services.scss',
})
export class AdminServices implements OnInit {
  readonly defaultProfileFields = [
    { key: 'FULL_NAME', label: 'Họ và tên', note: 'Tự động lấy từ thông tin tài khoản, khách không sửa tại biểu mẫu.' },
    { key: 'MOBILE_PHONE', label: 'Số điện thoại', note: 'Tự động lấy từ thông tin tài khoản, khách không sửa tại biểu mẫu.' },
    { key: 'PERMANENT_ADDRESS', label: 'Địa chỉ thường trú', note: 'Tự điền từ hồ sơ; khách có thể cập nhật và hệ thống sẽ ghi nhớ.' },
    { key: 'CONTACT_ADDRESS', label: 'Địa chỉ tạm trú / nơi ở hiện tại', note: 'Tự điền từ hồ sơ; khách có thể cập nhật và hệ thống sẽ ghi nhớ.' },
  ];
  private serviceApi = inject(AdminServicesService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  services: Service[] = [];
  selectedService: Service | null = null;
  draftFields: FormFieldDefinition[] = [];
  isLoading = true;
  isSaving = false;
  successMessage = '';
  errorMessage = '';
  readonly fieldTypes: { value: FormFieldType; label: string }[] = [
    { value: 'TEXT', label: 'Nhập một dòng' }, { value: 'TEXTAREA', label: 'Nhập nhiều dòng' },
    { value: 'NUMBER', label: 'Nhập số tiền hoặc số lượng' }, { value: 'DATE', label: 'Chọn ngày' },
    { value: 'SELECT', label: 'Chọn trong danh sách' }, { value: 'RADIO', label: 'Chọn một phương án' },
    { value: 'CHECKBOX', label: 'Đánh dấu Có hoặc Không' },
  ];

  ngOnInit(): void { this.loadServices(); }

  loadServices(): void {
    this.isLoading = true;
    this.serviceApi.getAllServices().subscribe({
      next: (services) => { this.services = this.sortServices(services || []); this.isLoading = false; if (!this.selectedService && this.services.length) this.select(this.services[0]); this.cdr.detectChanges(); },
      error: (error) => { this.errorMessage = this.apiError.getMessage(error, 'Không tải được dịch vụ.'); this.isLoading = false; this.cdr.detectChanges(); },
    });
  }

  private sortServices(services: Service[]): Service[] {
    return [...services].sort((left, right) => left.serviceName.localeCompare(right.serviceName, 'vi'));
  }

  select(service: Service): void {
    this.selectedService = service;
    this.draftFields = (service.formSchema || []).map((field) => ({ ...field, options: [...(field.options || [])] }));
    this.successMessage = ''; this.errorMessage = '';
  }

  addField(): void {
    const key = `field_${Date.now()}`;
    this.draftFields.push({ key, label: 'Trường mới', type: 'TEXT', required: false, placeholder: '', section: 'Thông tin giao dịch', options: [] });
  }

  removeField(index: number): void { this.draftFields.splice(index, 1); }
  move(index: number, direction: number): void { const target = index + direction; if (target < 0 || target >= this.draftFields.length) return; [this.draftFields[index], this.draftFields[target]] = [this.draftFields[target], this.draftFields[index]]; }
  needsOptions(field: FormFieldDefinition): boolean { return field.type === 'SELECT' || field.type === 'RADIO'; }
  fieldTypeLabel(field: FormFieldDefinition): string { return this.fieldTypes.find((item) => item.value === field.type)?.label || 'Ô nhập'; }
  optionsText(field: FormFieldDefinition): string { return (field.options || []).join(' | '); }
  setOptions(field: FormFieldDefinition, value: string): void { field.options = value.split('|').map((item) => item.trim()).filter(Boolean); }

  save(): void {
    if (!this.selectedService || this.isSaving) return;
    if (this.draftFields.some((field) => !field.key.trim() || !field.label.trim())) { this.errorMessage = 'Mã trường và nhãn hiển thị không được để trống.'; return; }
    const keys = this.draftFields.map((field) => field.key);
    if (new Set(keys).size !== keys.length) { this.errorMessage = 'Mã trường không được trùng nhau.'; return; }
    this.isSaving = true;
    const payload = {
      ...this.selectedService,
      formSchema: this.draftFields,
      requiredCustomerFields: [...new Set([
        ...this.defaultProfileFields.map((field) => field.key),
        ...(this.selectedService.requiredCustomerFields || []),
      ])],
    };
    this.serviceApi.updateService(this.selectedService.serviceId, payload).subscribe({
      next: (saved: any) => { const index = this.services.findIndex((item) => item.serviceId === saved.serviceId); this.services[index] = saved; this.selectedService = saved; this.draftFields = saved.formSchema || []; this.successMessage = 'Đã lưu và áp dụng biểu mẫu.'; this.errorMessage = ''; this.isSaving = false; this.cdr.detectChanges(); },
      error: (error) => { this.errorMessage = this.apiError.getMessage(error, 'Không lưu được biểu mẫu.'); this.isSaving = false; this.cdr.detectChanges(); },
    });
  }

  toggleService(): void {
    if (!this.selectedService) return;
    this.selectedService.status = this.selectedService.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.save();
  }
}
