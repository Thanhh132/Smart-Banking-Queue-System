import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SmartBranchRecommendation } from '../../../core/models/branch.model';
import { Service, ServiceCatalogItem } from '../../../core/models/service.model';
import { AdminServicesService } from '../../../core/services/admin-services.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { BranchService } from '../../../core/services/branch.service';
import { Delegation, DelegationService } from '../../../core/services/delegation.service';
import { LocationService } from '../../../core/services/location.service';
import { ServicesService } from '../../../core/services/services.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({ selector: 'app-customer-delegations', standalone: true,
  imports: [CommonModule, FormsModule, DashboardLayout],
  templateUrl: './customer-delegations.html', styleUrl: './customer-delegations.scss' })
export class CustomerDelegations implements OnInit {
  private api = inject(DelegationService); private catalogApi = inject(AdminServicesService);
  private branchApi = inject(BranchService); private servicesApi = inject(ServicesService);
  private locationApi = inject(LocationService); private errors = inject(ApiErrorService); private cdr = inject(ChangeDetectorRef);
  private readonly blockedCodes = new Set(['ACCOUNT_OPEN', 'DEBIT_CARD_NEW', 'CREDIT_CARD', 'DIGITAL_BANKING', 'IDENTITY_UPDATE', 'SIGNATURE_UPDATE']);

  catalog: ServiceCatalogItem[] = []; recommendations: SmartBranchRecommendation[] = [];
  delegations: Delegation[] = []; selectedCatalogId: number | null = null;
  selectedBranch: SmartBranchRecommendation | null = null; selectedBranchService: Service | null = null;
  isLocating = false; isRouting = false; isSaving = false; successMessage = ''; errorMessage = '';
  form = { delegateName: '', delegateIdentityNumber: '', delegateDateOfBirth: '', delegatePhone: '', identityIssueDate: '', identityExpiryDate: '', identityIssuePlace: '', relationship: '', transactionScope: '', validUntil: '', acceptedTerms: false };

  ngOnInit(): void {
    this.load();
    this.catalogApi.getCatalog().subscribe({ next: (items) => { this.catalog = items.filter((x) => x.status === 'ACTIVE'); this.cdr.detectChanges(); }, error: (e) => { this.errorMessage = this.errors.getMessage(e, 'Không tải được danh mục nghiệp vụ.'); this.cdr.detectChanges(); } });
  }
  get selectedCatalog(): ServiceCatalogItem | undefined { return this.catalog.find((x) => x.catalogId === this.selectedCatalogId); }
  get canDelegateSelected(): boolean { return !!this.selectedCatalog && !this.blockedCodes.has(this.selectedCatalog.serviceCode); }
  get maxValidUntil(): string { return this.localDateTime(new Date(Date.now() + 30 * 86400000)); }
  get minValidUntil(): string { return this.localDateTime(new Date(Date.now() + 30 * 60000)); }
  get today(): string { return new Date().toISOString().slice(0, 10); }

  serviceChanged(): void {
    this.recommendations = []; this.selectedBranch = null; this.selectedBranchService = null; this.errorMessage = '';
    if (!this.canDelegateSelected) return;
    const latitude = Number(sessionStorage.getItem('customerLatitude'));
    const longitude = Number(sessionStorage.getItem('customerLongitude'));
    if (Number.isFinite(latitude) && Number.isFinite(longitude) && latitude !== 0 && longitude !== 0) this.loadRecommendations(latitude, longitude);
  }
  useCurrentLocation(): void {
    if (!navigator.geolocation || !this.canDelegateSelected) return;
    this.isLocating = true;
    navigator.geolocation.getCurrentPosition((position) => {
      const { latitude, longitude } = position.coords;
      sessionStorage.setItem('customerLatitude', String(latitude)); sessionStorage.setItem('customerLongitude', String(longitude));
      this.isLocating = false; this.loadRecommendations(latitude, longitude);
    }, () => { this.isLocating = false; this.errorMessage = 'Không lấy được vị trí. Hãy bật quyền vị trí và thử lại.'; this.cdr.detectChanges(); }, { enableHighAccuracy: true, timeout: 10000 });
  }
  selectBranch(branch: SmartBranchRecommendation): void {
    if (!this.selectedCatalog) return;
    this.servicesApi.getMappedServicesByBranch(branch.branchId).subscribe({
      next: (services) => {
        const service = services.find((x) => x.serviceCode === this.selectedCatalog?.serviceCode);
        if (!service) { this.errorMessage = 'Chi nhánh vừa thay đổi cấu hình và không còn cung cấp nghiệp vụ này.'; return; }
        this.selectedBranch = branch; this.selectedBranchService = service; this.errorMessage = ''; this.cdr.detectChanges();
      }, error: (e) => { this.errorMessage = this.errors.getMessage(e, 'Không xác nhận được dịch vụ tại chi nhánh.'); this.cdr.detectChanges(); }
    });
  }
  changeBranch(): void { this.selectedBranch = null; this.selectedBranchService = null; }
  mapUrl(branch: SmartBranchRecommendation): string { return this.locationApi.googleMapsUrl(branch); }
  load(): void { this.api.getMine().subscribe({ next: (items) => { this.delegations = items; this.cdr.detectChanges(); }, error: (e) => { this.errorMessage = this.errors.getMessage(e, 'Không tải được danh sách ủy quyền.'); this.cdr.detectChanges(); } }); }
  create(): void {
    if (!this.selectedBranch || !this.selectedBranchService || this.isSaving) return;
    this.isSaving = true; this.errorMessage = '';
    const payload = { ...this.form, branchId: this.selectedBranch.branchId, serviceId: this.selectedBranchService.serviceId };
    this.api.create(payload).subscribe({ next: (item) => { this.delegations = [item, ...this.delegations]; this.isSaving = false; this.successMessage = `Đã tạo mã ${item.referenceCode}. Chỉ cung cấp mã cho đúng người được ủy quyền.`; this.reset(); this.cdr.detectChanges(); }, error: (e) => { this.errorMessage = this.errors.getMessage(e, 'Không tạo được ủy quyền.'); this.isSaving = false; this.cdr.detectChanges(); } });
  }
  cancel(item: Delegation): void { if (!confirm(`Hủy ủy quyền ${item.referenceCode}?`)) return; this.api.cancel(item.delegationId).subscribe({ next: (saved) => { this.delegations = this.delegations.map((x) => x.delegationId === saved.delegationId ? saved : x); this.cdr.detectChanges(); }, error: (e) => { this.errorMessage = this.errors.getMessage(e, 'Không hủy được ủy quyền.'); this.cdr.detectChanges(); } }); }
  statusLabel(status: string): string { return ({ ACTIVE: 'Đang hiệu lực', VERIFIED: 'Đã xác minh', USED: 'Đã sử dụng', CANCELLED: 'Đã hủy', EXPIRED: 'Hết hạn' } as Record<string,string>)[status] || status; }
  private loadRecommendations(latitude: number, longitude: number): void {
    if (!this.selectedCatalog) return; this.isRouting = true;
    this.branchApi.getSmartRecommendations('ALL', latitude, longitude, this.selectedCatalog.serviceCode).subscribe({ next: (items) => { this.recommendations = items.slice(0, 3); this.isRouting = false; if (!items.length) this.errorMessage = 'Hiện không có chi nhánh nào đang mở và cung cấp nghiệp vụ này.'; this.cdr.detectChanges(); }, error: (e) => { this.errorMessage = this.errors.getMessage(e, 'Không tải được gợi ý chi nhánh.'); this.isRouting = false; this.cdr.detectChanges(); } });
  }
  private reset(): void { this.selectedCatalogId = null; this.recommendations = []; this.selectedBranch = null; this.selectedBranchService = null; this.form = { delegateName: '', delegateIdentityNumber: '', delegateDateOfBirth: '', delegatePhone: '', identityIssueDate: '', identityExpiryDate: '', identityIssuePlace: '', relationship: '', transactionScope: '', validUntil: '', acceptedTerms: false }; }
  private localDateTime(date: Date): string { const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000); return local.toISOString().slice(0, 16); }
}
