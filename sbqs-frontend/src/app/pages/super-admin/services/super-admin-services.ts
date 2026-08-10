import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ServiceCatalogItem } from '../../../core/models/service.model';
import { AdminServicesService } from '../../../core/services/admin-services.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { ExcelImportPanel } from '../../../shared/components/excel-import-panel/excel-import-panel';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';

@Component({
  selector: 'app-super-admin-services',
  standalone: true,
  imports: [CommonModule, FormsModule, DashboardLayout, ExcelImportPanel, AppIcon],
  templateUrl: './super-admin-services.html',
  styleUrl: './super-admin-services.scss',
})
export class SuperAdminServices implements OnInit {
  private api = inject(AdminServicesService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);
  catalog: ServiceCatalogItem[] = [];
  editingCatalogId: number | null = null;
  deletingCatalogId: number | null = null;
  searchTerm = '';
  statusFilter = 'ALL';
  typeFilter = 'ALL';
  isLoading = true;
  isSaving = false;
  successMessage = '';
  errorMessage = '';
  form = {
    serviceCode: '',
    serviceName: '',
    serviceType: '',
    description: '',
    estimatedTime: 15,
    delegatable: false,
  };

  get activeCatalog(): ServiceCatalogItem[] {
    return this.catalog.filter((item) => item.status === 'ACTIVE');
  }
  get archivedCatalog(): ServiceCatalogItem[] {
    return this.catalog.filter((item) => item.status !== 'ACTIVE');
  }
  get delegatableCount(): number {
    return this.activeCatalog.filter((item) => item.delegatable).length;
  }
  get serviceTypes(): string[] {
    return [...new Set(this.catalog.map((item) => item.serviceType).filter(Boolean))].sort((a, b) =>
      a.localeCompare(b, 'vi'),
    );
  }
  get filteredCatalog(): ServiceCatalogItem[] {
    const term = this.searchTerm.trim().toLocaleLowerCase('vi');
    return this.catalog.filter((item) => {
      const matchesStatus =
        this.statusFilter === 'ALL' ||
        (this.statusFilter === 'ACTIVE' ? item.status === 'ACTIVE' : item.status !== 'ACTIVE');
      const matchesType = this.typeFilter === 'ALL' || item.serviceType === this.typeFilter;
      const searchable =
        `${item.serviceCode} ${item.serviceName} ${item.serviceType} ${item.description || ''}`.toLocaleLowerCase(
          'vi',
        );
      return matchesStatus && matchesType && (!term || searchable.includes(term));
    });
  }

  ngOnInit(): void {
    this.loadCatalog();
  }
  loadCatalog(): void {
    this.isLoading = true;
    this.api.getCatalog().subscribe({
      next: (items) => {
        this.catalog = items || [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.errorMessage = this.apiError.getMessage(error, 'Không tải được danh mục dịch vụ.');
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }
  create(): void {
    if (this.isSaving || !this.form.serviceName.trim() || !this.form.serviceType.trim()) return;
    this.isSaving = true;
    const request =
      this.editingCatalogId === null
        ? this.api.createCatalogItem({ ...this.form })
        : this.api.updateCatalogItem(this.editingCatalogId, { ...this.form });
    request.subscribe({
      next: (created) => {
        this.catalog = [
          ...this.catalog.filter((item) => item.catalogId !== created.catalogId),
          created,
        ].sort((a, b) => a.serviceName.localeCompare(b.serviceName, 'vi'));
        const wasEditing = this.editingCatalogId !== null;
        this.resetForm();
        this.successMessage = wasEditing
          ? 'Đã cập nhật dịch vụ dùng chung.'
          : 'Đã tạo và tự động đồng bộ dịch vụ đến tất cả chi nhánh.';
        this.errorMessage = '';
        this.isSaving = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.errorMessage = this.apiError.getMessage(error, 'Không tạo được dịch vụ.');
        this.isSaving = false;
        this.cdr.detectChanges();
      },
    });
  }

  edit(item: ServiceCatalogItem): void {
    this.editingCatalogId = item.catalogId;
    this.form = {
      serviceCode: item.serviceCode,
      serviceName: item.serviceName,
      serviceType: item.serviceType,
      description: item.description || '',
      estimatedTime: item.estimatedTime,
      delegatable: item.delegatable,
    };
  }

  resetForm(): void {
    this.editingCatalogId = null;
    this.form = {
      serviceCode: '',
      serviceName: '',
      serviceType: '',
      description: '',
      estimatedTime: 15,
      delegatable: false,
    };
  }

  remove(item: ServiceCatalogItem): void {
    if (
      this.deletingCatalogId !== null ||
      !confirm(`Xóa dịch vụ “${item.serviceName}” khỏi toàn bộ hệ thống?`)
    )
      return;
    this.deletingCatalogId = item.catalogId;
    this.errorMessage = '';
    this.api.deleteCatalogItem(item.catalogId).subscribe({
      next: () => {
        if (this.editingCatalogId === item.catalogId) this.resetForm();
        this.successMessage =
          'Đã xóa dịch vụ khỏi hệ thống vận hành và vẫn giữ nguyên lịch sử đã phát sinh.';
        this.deletingCatalogId = null;
        this.loadCatalog();
      },
      error: (error) => {
        this.errorMessage = this.apiError.getMessage(error, 'Không xóa được dịch vụ.');
        this.deletingCatalogId = null;
        this.cdr.detectChanges();
      },
    });
  }

  restore(item: ServiceCatalogItem): void {
    if (this.isSaving) return;
    this.isSaving = true;
    this.api.restoreCatalogItem(item.catalogId).subscribe({
      next: () => {
        this.successMessage = 'Đã khôi phục dịch vụ và đồng bộ lại đến các chi nhánh.';
        this.isSaving = false;
        this.loadCatalog();
      },
      error: (error) => {
        this.errorMessage = this.apiError.getMessage(error, 'Không khôi phục được dịch vụ.');
        this.isSaving = false;
        this.cdr.detectChanges();
      },
    });
  }

  imported(): void {
    this.successMessage =
      'Đã nhập danh mục và tự động đồng bộ các dòng hợp lệ đến tất cả chi nhánh.';
    this.loadCatalog();
  }
}
