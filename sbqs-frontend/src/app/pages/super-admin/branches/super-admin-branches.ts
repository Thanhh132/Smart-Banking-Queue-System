import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { forkJoin } from 'rxjs';

import {
  DistrictOption,
  ProvinceOption,
  VIETNAM_LOCATIONS,
} from '../../../core/data/vietnam-locations';
import { Branch } from '../../../core/models/branch.model';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { BranchService } from '../../../core/services/branch.service';
import { Service } from '../../../core/models/service.model';
import { AdminServicesService } from '../../../core/services/admin-services.service';
import { ManagedUser, UserManagementService } from '../../../core/services/user-management.service';
import { GeocodeResult, LocationService } from '../../../core/services/location.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppButton } from '../../../shared/components/app-button/app-button';
import { AppConfirmDialog } from '../../../shared/components/app-confirm-dialog/app-confirm-dialog';
import { AppDataTableShell } from '../../../shared/components/app-data-table-shell/app-data-table-shell';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';
import { AppModalShell } from '../../../shared/components/app-modal-shell/app-modal-shell';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { AppStatusBadge } from '../../../shared/components/app-status-badge/app-status-badge';
import { PreventAutofillDirective } from '../../../shared/directives/prevent-autofill.directive';
import { PASSWORD_POLICY_PATTERN } from '../../../shared/utils/password-policy.util';

@Component({
  selector: 'app-super-admin-branches',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    DashboardLayout,
    AppButton,
    AppConfirmDialog,
    AppDataTableShell,
    AppIcon,
    AppModalShell,
    AppPageHeader,
    AppStatusBadge,
    PreventAutofillDirective,
  ],
  templateUrl: './super-admin-branches.html',
  styleUrl: './super-admin-branches.scss',
})
export class SuperAdminBranches implements OnInit {
  private fb = inject(FormBuilder);
  private branchService = inject(BranchService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);
  private locationService = inject(LocationService);
  private sanitizer = inject(DomSanitizer);
  private userService = inject(UserManagementService);
  private servicesApi = inject(AdminServicesService);
  private isHydratingForm = false;

  branches: Branch[] = [];
  isLoading = false;
  isSubmitting = false;
  isGeocoding = false;
  locationConfirmed = false;
  isEditMode = false;
  editingBranchId: number | null = null;
  successMessage = '';
  errorMessage = '';
  searchTerm = '';
  mapPreviewUrl: SafeResourceUrl = '';
  isEditorOpen = false;
  pendingDeleteBranch: Branch | null = null;
  isDeleting = false;
  expandedBranchId: number | null = null;
  detailTab: 'admins' | 'staff' | 'services' = 'admins';
  detailLoadingBranchId: number | null = null;
  branchUsers: Record<number, ManagedUser[]> = {};
  branchServices: Record<number, Service[]> = {};
  adminEditorOpen = false;
  adminEditorBranch: Branch | null = null;
  editingAdminId: number | null = null;
  isAdminSaving = false;
  pendingDeleteAdmin: ManagedUser | null = null;

  adminForm = this.fb.group({
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', Validators.required],
    password: ['', [Validators.required, Validators.pattern(PASSWORD_POLICY_PATTERN)]],
    confirmPassword: ['', Validators.required],
  });

  readonly bankOptions = [
    { label: 'BIDV', value: 'BIDV', code: 'BIDV' },
    { label: 'Vietcombank', value: 'Vietcombank', code: 'VCB' },
    { label: 'VietinBank', value: 'VietinBank', code: 'VTB' },
    { label: 'Agribank', value: 'Agribank', code: 'AGB' },
    { label: 'Techcombank', value: 'Techcombank', code: 'TCB' },
    { label: 'MB Bank', value: 'MB Bank', code: 'MB' },
    { label: 'ACB', value: 'ACB', code: 'ACB' },
    { label: 'Sacombank', value: 'Sacombank', code: 'STB' },
    { label: 'VPBank', value: 'VPBank', code: 'VPB' },
  ];
  readonly provinceOptions: ProvinceOption[] = VIETNAM_LOCATIONS;

  branchForm = this.fb.group({
    bankName: ['BIDV', [Validators.required]],
    branchCode: [{ value: '', disabled: true }],
    branchName: ['', [Validators.required]],
    province: [''],
    district: [''],
    ward: [''],
    address: ['', [Validators.required]],
    phone: ['', [Validators.required, Validators.pattern(/^[0-9+\s.-]{8,15}$/)]],
    latitude: this.fb.control<number | null>(null, [Validators.required]),
    longitude: this.fb.control<number | null>(null, [Validators.required]),
    status: ['ACTIVE', [Validators.required]],
  });

  get filteredBranches(): Branch[] {
    const keyword = this.searchTerm.trim().toLocaleLowerCase('vi');
    if (!keyword) return this.branches;
    return this.branches.filter((branch) =>
      [
        branch.bankName,
        branch.branchCode,
        branch.branchName,
        branch.province,
        branch.district,
        branch.ward,
      ]
        .filter(Boolean)
        .some((value) => String(value).toLocaleLowerCase('vi').includes(keyword)),
    );
  }

  get activeBranchCount(): number {
    return this.branches.filter((branch) => branch.status === 'ACTIVE').length;
  }

  branchAdmins(branchId: number): ManagedUser[] {
    return (this.branchUsers[branchId] || []).filter((user) => user.role === 'BRANCH_ADMIN');
  }

  branchStaff(branchId: number): ManagedUser[] {
    return (this.branchUsers[branchId] || []).filter((user) => user.role === 'STAFF');
  }

  activeServices(branchId: number): Service[] {
    return (this.branchServices[branchId] || []).filter((service) => service.status === 'ACTIVE');
  }

  inactiveServices(branchId: number): Service[] {
    return (this.branchServices[branchId] || []).filter((service) => service.status !== 'ACTIVE');
  }

  toggleBranchDetails(branch: Branch): void {
    if (this.expandedBranchId === branch.branchId) {
      this.expandedBranchId = null;
      return;
    }
    this.expandedBranchId = branch.branchId;
    this.detailTab = 'admins';
    if (this.branchUsers[branch.branchId] && this.branchServices[branch.branchId]) return;
    this.detailLoadingBranchId = branch.branchId;
    forkJoin({
      users: this.userService.getUsersByBranch(branch.branchId),
      services: this.servicesApi.getServicesByBranch(branch.branchId),
    }).subscribe({
      next: ({ users, services }) => {
        this.branchUsers[branch.branchId] = users || [];
        this.branchServices[branch.branchId] = services || [];
        this.detailLoadingBranchId = null;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.errorMessage = this.apiError.getMessage(error, 'Không tải được chi tiết chi nhánh.');
        this.detailLoadingBranchId = null;
        this.cdr.detectChanges();
      },
    });
  }

  selectDetailTab(tab: 'admins' | 'staff' | 'services'): void {
    this.detailTab = tab;
  }

  openAdminEditor(branch: Branch, admin?: ManagedUser): void {
    this.adminEditorBranch = branch;
    this.editingAdminId = admin?.userId ?? null;
    const password = this.adminForm.controls.password;
    const confirmation = this.adminForm.controls.confirmPassword;
    if (admin) {
      password.clearValidators();
      confirmation.clearValidators();
    } else {
      password.setValidators([Validators.required, Validators.pattern(PASSWORD_POLICY_PATTERN)]);
      confirmation.setValidators(Validators.required);
    }
    password.updateValueAndValidity();
    confirmation.updateValueAndValidity();
    this.adminForm.reset({
      fullName: admin?.fullName || '',
      email: admin?.email || '',
      phone: admin?.phone || '',
      password: '',
      confirmPassword: '',
    });
    this.adminEditorOpen = true;
  }

  closeAdminEditor(): void {
    if (this.isAdminSaving) return;
    this.adminEditorOpen = false;
    this.adminEditorBranch = null;
    this.editingAdminId = null;
  }

  saveAdmin(): void {
    const branch = this.adminEditorBranch;
    if (!branch || this.adminForm.invalid || this.isAdminSaving) {
      this.adminForm.markAllAsTouched();
      return;
    }
    if (!this.editingAdminId && this.adminForm.value.password !== this.adminForm.value.confirmPassword) {
      this.errorMessage = 'Mật khẩu xác nhận không khớp.';
      return;
    }
    const common = {
      fullName: this.adminForm.value.fullName,
      email: this.adminForm.value.email,
      phone: this.adminForm.value.phone,
      branchId: branch.branchId,
    };
    const request = this.editingAdminId
      ? this.userService.updateUser(this.editingAdminId, common)
      : this.userService.createAdminBranch({
          ...common,
          password: this.adminForm.value.password,
          confirmPassword: this.adminForm.value.confirmPassword,
        });
    this.isAdminSaving = true;
    request.subscribe({
      next: () => {
        this.successMessage = this.editingAdminId ? 'Đã cập nhật quản trị viên.' : 'Đã thêm quản trị viên cho chi nhánh.';
        this.isAdminSaving = false;
        delete this.branchUsers[branch.branchId];
        delete this.branchServices[branch.branchId];
        this.closeAdminEditor();
        this.expandedBranchId = null;
        this.toggleBranchDetails(branch);
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.errorMessage = this.apiError.getMessage(error, 'Không lưu được quản trị viên.');
        this.isAdminSaving = false;
        this.cdr.detectChanges();
      },
    });
  }

  deleteAdmin(admin: ManagedUser): void {
    this.pendingDeleteAdmin = admin;
  }

  confirmDeleteAdmin(): void {
    const admin = this.pendingDeleteAdmin;
    if (!admin) return;
    this.userService.deleteUser(admin.userId).subscribe({
      next: () => {
        const branchId = admin.branch?.branchId;
        if (branchId) this.branchUsers[branchId] = (this.branchUsers[branchId] || []).filter((user) => user.userId !== admin.userId);
        this.pendingDeleteAdmin = null;
        this.successMessage = 'Đã xóa quản trị viên khỏi chi nhánh.';
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.errorMessage = this.apiError.getMessage(error, 'Không xóa được quản trị viên.');
        this.cdr.detectChanges();
      },
    });
  }

  get googleMapsUrl(): string {
    return this.locationService.googleMapsUrl({
      latitude: this.branchForm.get('latitude')?.value ?? undefined,
      longitude: this.branchForm.get('longitude')?.value ?? undefined,
      address: this.fullAddress,
      branchName: this.branchForm.get('branchName')?.value || '',
    });
  }

  ngOnInit(): void {
    this.branchForm.get('bankName')?.valueChanges.subscribe(() => this.syncGeneratedFields());
    this.syncGeneratedFields();
    this.updateMapPreview();
    this.loadBranches();
  }

  loadBranches(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.branchService.getBranches().subscribe({
      next: (branches) => {
        this.branches = branches || [];
        this.isLoading = false;
        this.syncGeneratedFields();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không tải được danh sách chi nhánh.');
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  /** Bắt buộc xác nhận tọa độ trước khi lưu để dữ liệu tìm chi nhánh gần nhất luôn đáng tin cậy. */
  submitBranch(): void {
    if (!this.locationConfirmed) {
      const requiredSourceControls = ['bankName', 'address', 'phone', 'status'];
      const sourceInvalid = requiredSourceControls.some(
        (name) => this.branchForm.get(name)?.invalid,
      );
      if (sourceInvalid) {
        this.branchForm.markAllAsTouched();
        this.cdr.detectChanges();
        return;
      }
      this.resolveLocation(true);
      return;
    }

    if (this.branchForm.invalid) {
      this.branchForm.markAllAsTouched();
      this.cdr.detectChanges();
      return;
    }

    this.persistBranch();
  }

  startEdit(branch: Branch): void {
    this.isEditMode = true;
    this.editingBranchId = branch.branchId;
    this.isHydratingForm = true;

    this.branchForm.patchValue({
      bankName: branch.bankName,
      branchCode: branch.branchCode,
      branchName: branch.branchName,
      province: branch.province || '',
      district: branch.district || '',
      ward: branch.ward || '',
      address: branch.address,
      phone: branch.phone,
      latitude: branch.latitude ?? null,
      longitude: branch.longitude ?? null,
      status: branch.status || 'ACTIVE',
    });
    this.isHydratingForm = false;
    this.locationConfirmed = branch.latitude != null && branch.longitude != null;
    this.isEditorOpen = true;
    this.updateMapPreview();
    this.cdr.detectChanges();
  }

  deleteBranch(branch: Branch): void {
    if (this.isDeleting) return;
    this.pendingDeleteBranch = branch;
  }

  confirmDeleteBranch(): void {
    const branch = this.pendingDeleteBranch;
    if (!branch || this.isDeleting) return;
    this.successMessage = '';
    this.errorMessage = '';
    this.isDeleting = true;
    this.branchService.deleteBranch(branch.branchId).subscribe({
      next: () => {
        this.successMessage = 'Đã xóa chi nhánh.';
        this.isDeleting = false;
        this.pendingDeleteBranch = null;
        this.loadBranches();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không xóa được chi nhánh đang được sử dụng.',
        );
        this.isDeleting = false;
        this.cdr.detectChanges();
      },
    });
  }

  cancelDeleteBranch(): void {
    if (!this.isDeleting) {
      this.pendingDeleteBranch = null;
    }
  }

  openCreate(): void {
    this.cancelEdit();
    this.successMessage = '';
    this.errorMessage = '';
    this.isEditorOpen = true;
  }

  cancelEdit(): void {
    if (this.isSubmitting || this.isGeocoding) return;
    this.isEditMode = false;
    this.editingBranchId = null;
    this.isHydratingForm = true;
    this.branchForm.reset({
      bankName: 'BIDV',
      branchCode: '',
      branchName: '',
      province: '',
      district: '',
      ward: '',
      address: '',
      phone: '',
      latitude: null,
      longitude: null,
      status: 'ACTIVE',
    });
    this.isHydratingForm = false;
    this.locationConfirmed = false;
    this.isEditorOpen = false;
    this.syncGeneratedFields();
    this.updateMapPreview();
    this.cdr.detectChanges();
  }

  updateMapPreview(): void {
    const latitude = this.branchForm.get('latitude')?.value;
    const longitude = this.branchForm.get('longitude')?.value;
    const query =
      latitude != null && longitude != null && this.shouldPreviewCoordinates()
        ? `${latitude},${longitude}`
        : this.addressQuery || 'Việt Nam';
    this.mapPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://www.google.com/maps?q=${encodeURIComponent(query)}&z=17&output=embed`,
    );
  }

  markLocationUnconfirmed(): void {
    this.locationConfirmed = false;
    this.branchForm.patchValue(
      {
        province: '',
        district: '',
        ward: '',
        latitude: null,
        longitude: null,
      },
      { emitEvent: false },
    );
    this.successMessage = '';
    this.updateMapPreview();
  }

  confirmMapLocation(): void {
    if (!this.branchForm.get('address')?.value?.trim()) return;
    this.resolveLocation(false);
  }

  branchMapUrl(branch: Branch): string {
    return this.locationService.googleMapsUrl(branch);
  }

  private persistBranch(): void {
    const payload = this.branchForm.getRawValue();
    this.isSubmitting = true;
    this.successMessage = '';
    this.errorMessage = '';
    const request$ =
      this.isEditMode && this.editingBranchId
        ? this.branchService.updateBranch(this.editingBranchId, payload)
        : this.branchService.createBranch(payload);

    request$.subscribe({
      next: () => {
        this.successMessage = this.isEditMode ? 'Đã cập nhật chi nhánh.' : 'Đã tạo chi nhánh mới.';
        this.isSubmitting = false;
        this.cancelEdit();
        this.loadBranches();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không lưu được chi nhánh.');
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  /**
   * Chuẩn hóa địa chỉ/link Maps/tọa độ qua geocoding; tùy ngữ cảnh có thể tiếp tục
   * lưu ngay sau khi kết quả hành chính và tọa độ đã được xác nhận.
   */
  private resolveLocation(saveAfterResolved: boolean): void {
    if (!this.addressQuery) {
      this.errorMessage = 'Vui lòng nhập địa chỉ chi nhánh.';
      return;
    }

    this.isGeocoding = true;
    this.errorMessage = '';
    this.locationService.geocode(this.addressQuery).subscribe({
      next: (result) => {
        const administrative = this.resolveAdministrativeFields(result);
        const shouldUseResolvedAddress =
          this.looksLikeMapUrlOrCoordinates(this.fullAddress) &&
          !!result.formattedAddress &&
          !this.looksLikeMapUrlOrCoordinates(result.formattedAddress);
        this.isHydratingForm = true;
        this.branchForm.patchValue(
          {
            address: shouldUseResolvedAddress ? result.formattedAddress : this.fullAddress,
            province: administrative.province,
            district: administrative.district,
            ward: administrative.ward,
            latitude: result.latitude,
            longitude: result.longitude,
          },
          { emitEvent: false },
        );
        this.isHydratingForm = false;
        this.syncGeneratedFields();
        this.locationConfirmed = true;
        this.isGeocoding = false;
        this.updateMapPreview();
        this.successMessage = saveAfterResolved
          ? 'Đã xác định vị trí. Đang lưu chi nhánh...'
          : 'Đã xác định vị trí chi nhánh.';
        this.cdr.detectChanges();
        if (saveAfterResolved) {
          this.persistBranch();
        }
      },
      error: (err) => {
        this.isGeocoding = false;
        this.locationConfirmed = false;
        this.errorMessage = this.apiError.getMessage(
          err,
          'Không tìm thấy địa chỉ này. Hãy nhập chi tiết hơn hoặc dán link Google Maps/toạ độ.',
        );
        this.cdr.detectChanges();
      },
    });
  }

  private get fullAddress(): string {
    return this.branchForm.get('address')?.value?.trim() || '';
  }

  private get addressQuery(): string {
    const address = this.fullAddress;
    if (!address) return '';
    if (this.looksLikeMapUrlOrCoordinates(address)) return address;
    return this.normalizeText(address).includes('viet nam') ? address : `${address}, Việt Nam`;
  }

  /** Sinh mã và tên chi nhánh nhất quán từ ngân hàng + đơn vị hành chính khi tạo mới. */
  private syncGeneratedFields(): void {
    if (this.isEditMode || this.isHydratingForm) return;
    const bank = this.branchForm.get('bankName')?.value || 'BIDV';
    const districtName = this.branchForm.get('district')?.value?.trim() || '';
    const ward = this.branchForm.get('ward')?.value || '';
    const bankCode = this.bankOptions.find((item) => item.value === bank)?.code || 'BANK';
    const districtCode = this.findDistrictOption()?.code || this.createAreaCode(districtName);
    const prefix = `${bankCode}-${districtCode}-`;
    const sequence =
      this.branches.filter((branch) => branch.branchCode?.startsWith(prefix)).length + 1;
    this.branchForm.patchValue(
      {
        branchCode: `${prefix}${String(sequence).padStart(2, '0')}`,
        branchName: [bank, districtName, ward ? `- ${ward}` : ''].filter(Boolean).join(' '),
      },
      { emitEvent: false },
    );
  }

  private findDistrictOption(): DistrictOption | undefined {
    const province = this.branchForm.get('province')?.value || '';
    const district = this.branchForm.get('district')?.value || '';
    const provinceOption = this.provinceOptions.find((item) =>
      this.sameAdministrativeName(item.label, province),
    );
    return provinceOption?.districts.find((item) =>
      this.sameAdministrativeName(item.label, district),
    );
  }

  private resolveAdministrativeFields(result: GeocodeResult): {
    province: string;
    district: string;
    ward: string;
  } {
    const typedAddress = this.normalizeText(this.fullAddress);
    const provinceOption =
      this.provinceOptions.find((item) => typedAddress.includes(this.normalizeText(item.label))) ||
      this.provinceOptions.find((item) => this.sameAdministrativeName(item.label, result.province));
    const districtOption =
      provinceOption?.districts.find((item) =>
        typedAddress.includes(this.normalizeText(item.label)),
      ) ||
      provinceOption?.districts.find((item) =>
        this.sameAdministrativeName(item.label, result.district),
      );
    const wardOption = districtOption?.wards.find((item) =>
      typedAddress.includes(this.normalizeText(item.label)),
    );

    return {
      province: provinceOption?.label || result.province || '',
      district: districtOption?.label || result.district || '',
      ward: wardOption?.label || result.ward || '',
    };
  }

  private sameAdministrativeName(left: string, right: string): boolean {
    const clean = (value: string) =>
      this.normalizeText(value)
        .replace(/\b(thanh pho|tinh|quan|huyen|thi xa|phuong|xa)\b/g, '')
        .replace(/\s+/g, ' ')
        .trim();
    return clean(left) === clean(right);
  }

  private createAreaCode(value: string): string {
    const words = this.normalizeText(value)
      .replace(/\b(thanh pho|quan|huyen|thi xa)\b/g, '')
      .trim()
      .split(/\s+/)
      .filter(Boolean);
    if (!words.length) return 'BR';
    return (
      words.length === 1 ? words[0].slice(0, 3) : words.map((word) => word[0]).join('')
    ).toUpperCase();
  }

  private looksLikeMapUrlOrCoordinates(value: string): boolean {
    return /https?:\/\/|@-?\d|!3d-?\d|-?\d{1,2}(\.\d+)?,\s*-?\d{1,3}(\.\d+)?/.test(value);
  }

  private shouldPreviewCoordinates(): boolean {
    const address = this.fullAddress;
    if (!address) return true;
    return /@-?\d|!3d-?\d|-?\d{1,2}(\.\d+)?,\s*-?\d{1,3}(\.\d+)?/.test(address);
  }

  private normalizeText(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
  }
}
