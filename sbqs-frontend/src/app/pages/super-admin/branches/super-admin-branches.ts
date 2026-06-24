import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { DistrictOption, ProvinceOption, VIETNAM_LOCATIONS } from '../../../core/data/vietnam-locations';
import { Branch } from '../../../core/models/branch.model';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { BranchService } from '../../../core/services/branch.service';
import { GeocodeResult, LocationService } from '../../../core/services/location.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({
  selector: 'app-super-admin-branches',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DashboardLayout],
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
      [branch.bankName, branch.branchCode, branch.branchName, branch.province, branch.district, branch.ward]
        .filter(Boolean)
        .some((value) => String(value).toLocaleLowerCase('vi').includes(keyword))
    );
  }

  get activeBranchCount(): number {
    return this.branches.filter((branch) => branch.status === 'ACTIVE').length;
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

  submitBranch(): void {
    if (!this.locationConfirmed) {
      const requiredSourceControls = ['bankName', 'address', 'phone', 'status'];
      const sourceInvalid = requiredSourceControls.some((name) => this.branchForm.get(name)?.invalid);
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
    this.updateMapPreview();
    this.cdr.detectChanges();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  deleteBranch(branch: Branch): void {
    if (!confirm(`Xóa vĩnh viễn chi nhánh "${branch.branchName}"?`)) return;
    this.successMessage = '';
    this.errorMessage = '';
    this.branchService.deleteBranch(branch.branchId).subscribe({
      next: () => {
        this.successMessage = 'Đã xóa chi nhánh.';
        this.loadBranches();
      },
      error: (err) => {
        this.errorMessage = this.apiError.getMessage(err, 'Không xóa được chi nhánh đang được sử dụng.');
        this.cdr.detectChanges();
      },
    });
  }

  cancelEdit(): void {
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
    this.syncGeneratedFields();
    this.updateMapPreview();
    this.cdr.detectChanges();
  }

  updateMapPreview(): void {
    const latitude = this.branchForm.get('latitude')?.value;
    const longitude = this.branchForm.get('longitude')?.value;
    const query = latitude != null && longitude != null
      ? `${latitude},${longitude}`
      : this.addressQuery || 'Việt Nam';
    this.mapPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://www.google.com/maps?q=${encodeURIComponent(query)}&z=17&output=embed`
    );
  }

  markLocationUnconfirmed(): void {
    this.locationConfirmed = false;
    this.branchForm.patchValue({
      province: '',
      district: '',
      ward: '',
      latitude: null,
      longitude: null,
    }, { emitEvent: false });
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
    const request$ = this.isEditMode && this.editingBranchId
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
        this.isHydratingForm = true;
        this.branchForm.patchValue({
          province: administrative.province,
          district: administrative.district,
          ward: administrative.ward,
          latitude: result.latitude,
          longitude: result.longitude,
        }, { emitEvent: false });
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
          'Không tìm thấy địa chỉ này. Hãy nhập chi tiết hơn hoặc dán link Google Maps/toạ độ.'
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

  private syncGeneratedFields(): void {
    if (this.isEditMode || this.isHydratingForm) return;
    const bank = this.branchForm.get('bankName')?.value || 'BIDV';
    const districtName = this.branchForm.get('district')?.value?.trim() || '';
    const ward = this.branchForm.get('ward')?.value || '';
    const bankCode = this.bankOptions.find((item) => item.value === bank)?.code || 'BANK';
    const districtCode = this.findDistrictOption()?.code || this.createAreaCode(districtName);
    const prefix = `${bankCode}-${districtCode}-`;
    const sequence = this.branches.filter((branch) => branch.branchCode?.startsWith(prefix)).length + 1;
    this.branchForm.patchValue({
      branchCode: `${prefix}${String(sequence).padStart(2, '0')}`,
      branchName: [bank, districtName, ward ? `- ${ward}` : ''].filter(Boolean).join(' '),
    }, { emitEvent: false });
  }

  private findDistrictOption(): DistrictOption | undefined {
    const province = this.branchForm.get('province')?.value || '';
    const district = this.branchForm.get('district')?.value || '';
    const provinceOption = this.provinceOptions.find((item) =>
      this.sameAdministrativeName(item.label, province)
    );
    return provinceOption?.districts.find((item) =>
      this.sameAdministrativeName(item.label, district)
    );
  }

  private resolveAdministrativeFields(result: GeocodeResult): {
    province: string;
    district: string;
    ward: string;
  } {
    const typedAddress = this.normalizeText(this.fullAddress);
    const provinceOption = this.provinceOptions.find((item) =>
      typedAddress.includes(this.normalizeText(item.label))
    ) || this.provinceOptions.find((item) => this.sameAdministrativeName(item.label, result.province));
    const districtOption = provinceOption?.districts.find((item) =>
      typedAddress.includes(this.normalizeText(item.label))
    ) || provinceOption?.districts.find((item) => this.sameAdministrativeName(item.label, result.district));
    const wardOption = districtOption?.wards.find((item) =>
      typedAddress.includes(this.normalizeText(item.label))
    );

    return {
      province: provinceOption?.label || result.province || '',
      district: districtOption?.label || result.district || '',
      ward: wardOption?.label || result.ward || '',
    };
  }

  private sameAdministrativeName(left: string, right: string): boolean {
    const clean = (value: string) => this.normalizeText(value)
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
    return (words.length === 1 ? words[0].slice(0, 3) : words.map((word) => word[0]).join(''))
      .toUpperCase();
  }

  private looksLikeMapUrlOrCoordinates(value: string): boolean {
    return /https?:\/\/|@-?\d|!3d-?\d|-?\d{1,2}(\.\d+)?,\s*-?\d{1,3}(\.\d+)?/.test(value);
  }

  private normalizeText(value: string): string {
    return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  }
}
