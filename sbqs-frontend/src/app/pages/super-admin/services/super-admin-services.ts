import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ServiceCatalogItem } from '../../../core/models/service.model';
import { AdminServicesService } from '../../../core/services/admin-services.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({
  selector: 'app-super-admin-services', standalone: true,
  imports: [CommonModule, FormsModule, DashboardLayout],
  templateUrl: './super-admin-services.html', styleUrl: './super-admin-services.scss',
})
export class SuperAdminServices implements OnInit {
  private api = inject(AdminServicesService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);
  catalog: ServiceCatalogItem[] = [];
  isLoading = true; isSaving = false; successMessage = ''; errorMessage = '';
  form = { serviceCode: '', serviceName: '', serviceType: 'CARD', description: '', estimatedTime: 15 };

  ngOnInit(): void { this.loadCatalog(); }
  loadCatalog(): void {
    this.isLoading = true;
    this.api.getCatalog().subscribe({
      next: (items) => { this.catalog = items || []; this.isLoading = false; this.cdr.detectChanges(); },
      error: (error) => { this.errorMessage = this.apiError.getMessage(error, 'Không tải được danh mục dịch vụ.'); this.isLoading = false; this.cdr.detectChanges(); },
    });
  }
  create(): void {
    if (this.isSaving || !this.form.serviceCode.trim() || !this.form.serviceName.trim()) return;
    this.isSaving = true;
    this.api.createCatalogItem({ ...this.form }).subscribe({
      next: (created) => {
        this.catalog = [...this.catalog, created].sort((a, b) => a.serviceName.localeCompare(b.serviceName, 'vi'));
        this.form = { serviceCode: '', serviceName: '', serviceType: 'CARD', description: '', estimatedTime: 15 };
        this.successMessage = 'Đã tạo dịch vụ dùng chung. Tất cả Branch Admin có thể thêm dịch vụ này vào chi nhánh.';
        this.errorMessage = ''; this.isSaving = false; this.cdr.detectChanges();
      },
      error: (error) => { this.errorMessage = this.apiError.getMessage(error, 'Không tạo được dịch vụ.'); this.isSaving = false; this.cdr.detectChanges(); },
    });
  }
}
