import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { Branch } from '../../../core/models/branch.model';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { BranchService } from '../../../core/services/branch.service';
import { LocationService } from '../../../core/services/location.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({
  selector: 'app-branch-selection',
  imports: [CommonModule, FormsModule, DashboardLayout],
  templateUrl: './branch-selection.html',
  styleUrl: './branch-selection.scss',
})
export class BranchSelection implements OnInit {
  private branchService = inject(BranchService);
  private locationService = inject(LocationService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  branches: Branch[] = [];
  errorMessage = '';
  locationMessage = '';
  searchTerm = '';
  selectedBank = 'ALL';
  customerAddress = localStorage.getItem('customerAddress') || '';
  isLocating = false;
  private distances = new Map<number, number>();

  get bankOptions(): string[] {
    return [...new Set(this.branches.map((branch) => branch.bankName))].sort();
  }

  get filteredBranches(): Branch[] {
    const keyword = this.searchTerm.trim().toLocaleLowerCase('vi');
    return this.branches
      .filter((branch) => this.selectedBank === 'ALL' || branch.bankName === this.selectedBank)
      .filter((branch) => !keyword ||
        [branch.bankName, branch.branchName, branch.district, branch.province, branch.ward, branch.address]
          .filter(Boolean)
          .some((value) => String(value).toLocaleLowerCase('vi').includes(keyword)))
      .sort((first, second) =>
        (this.distances.get(first.branchId) ?? Number.MAX_VALUE)
        - (this.distances.get(second.branchId) ?? Number.MAX_VALUE));
  }

  ngOnInit(): void {
    this.branchService.getBranches().subscribe({
      next: (data) => {
        this.branches = (data || []).filter((branch) => branch.status === 'ACTIVE');
        const latitude = Number(localStorage.getItem('customerLatitude'));
        const longitude = Number(localStorage.getItem('customerLongitude'));
        if (latitude && longitude) this.setCustomerLocation(latitude, longitude, this.customerAddress);
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Không tải được danh sách chi nhánh.';
        this.cdr.detectChanges();
      },
    });
  }

  findNearbyByAddress(): void {
    if (!this.customerAddress.trim()) {
      this.errorMessage = 'Vui lòng nhập địa chỉ của bạn.';
      return;
    }

    this.isLocating = true;
    this.errorMessage = '';
    this.locationService.geocode(this.customerAddress).subscribe({
      next: (result) => {
        this.customerAddress = result.formattedAddress;
        this.setCustomerLocation(result.latitude, result.longitude, result.formattedAddress);
        this.isLocating = false;
        this.locationMessage = 'Đã sắp xếp chi nhánh theo khoảng cách gần nhất.';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLocating = false;
        this.errorMessage = this.apiError.getMessage(err, 'Không xác định được địa chỉ của bạn.');
        this.cdr.detectChanges();
      },
    });
  }

  useCurrentLocation(): void {
    if (!navigator.geolocation) {
      this.errorMessage = 'Trình duyệt không hỗ trợ xác định vị trí.';
      return;
    }

    this.isLocating = true;
    this.errorMessage = '';
    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.setCustomerLocation(position.coords.latitude, position.coords.longitude, 'Vị trí hiện tại');
        this.customerAddress = 'Vị trí hiện tại';
        this.isLocating = false;
        this.locationMessage = 'Đã sắp xếp chi nhánh theo vị trí hiện tại.';
        this.cdr.detectChanges();
      },
      () => {
        this.isLocating = false;
        this.errorMessage = 'Không thể lấy vị trí. Vui lòng cấp quyền vị trí cho trình duyệt.';
        this.cdr.detectChanges();
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  distanceLabel(branch: Branch): string {
    const distance = this.distances.get(branch.branchId);
    return distance == null ? '' : `${distance.toFixed(distance < 10 ? 1 : 0)} km`;
  }

  branchMapUrl(branch: Branch): string {
    return this.locationService.googleMapsUrl(branch);
  }

  selectBranch(branchId: number): void {
    localStorage.setItem('selectedBranchId', String(branchId));
    this.router.navigate(['/services']);
  }

  private setCustomerLocation(latitude: number, longitude: number, address: string): void {
    localStorage.setItem('customerAddress', address);
    localStorage.setItem('customerLatitude', String(latitude));
    localStorage.setItem('customerLongitude', String(longitude));
    this.distances.clear();
    for (const branch of this.branches) {
      if (branch.latitude != null && branch.longitude != null) {
        this.distances.set(branch.branchId, this.locationService.distanceInKm(
          latitude, longitude, branch.latitude, branch.longitude));
      }
    }
  }
}
