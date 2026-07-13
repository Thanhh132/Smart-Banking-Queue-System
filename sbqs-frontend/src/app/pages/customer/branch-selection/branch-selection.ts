import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { Branch } from '../../../core/models/branch.model';
import { BranchService } from '../../../core/services/branch.service';
import { LocationService } from '../../../core/services/location.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';

@Component({
  selector: 'app-branch-selection',
  imports: [CommonModule, FormsModule, DashboardLayout, AppIcon],
  templateUrl: './branch-selection.html',
  styleUrl: './branch-selection.scss',
})
export class BranchSelection implements OnInit {
  private branchService = inject(BranchService);
  private locationService = inject(LocationService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  branches: Branch[] = [];
  popup: { type: 'success' | 'error' | 'info'; title: string; message: string } | null = null;
  searchTerm = '';
  selectedBank = 'ALL';
  customerLocationLabel = sessionStorage.getItem('customerAddress') || '';
  isLocating = false;
  private distances = new Map<number, number>();
  private popupTimer: ReturnType<typeof setTimeout> | null = null;

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
        const latitude = Number(sessionStorage.getItem('customerLatitude'));
        const longitude = Number(sessionStorage.getItem('customerLongitude'));
        if (latitude && longitude) {
          this.setCustomerLocation(latitude, longitude, this.customerLocationLabel || 'Vị trí hiện tại');
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.showPopup('error', 'Không tải được chi nhánh', 'Vui lòng kiểm tra kết nối hoặc thử lại sau.');
        this.cdr.detectChanges();
      },
    });
  }

  useCurrentLocation(): void {
    if (!navigator.geolocation) {
      this.showPopup('error', 'Không hỗ trợ định vị', 'Trình duyệt hiện tại không hỗ trợ lấy vị trí tự động.');
      return;
    }

    this.isLocating = true;
    this.showPopup('info', 'Đang lấy vị trí', 'Nếu trình duyệt hỏi quyền truy cập vị trí, hãy chọn Cho phép.');
    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.setCustomerLocation(position.coords.latitude, position.coords.longitude, 'Vị trí hiện tại');
        this.customerLocationLabel = 'Vị trí hiện tại';
        this.isLocating = false;
        this.showPopup('success', 'Đã xác định vị trí', 'Danh sách chi nhánh đã được sắp xếp theo khoảng cách gần nhất.');
        this.cdr.detectChanges();
      },
      (error) => {
        this.isLocating = false;
        this.showPopup('error', 'Không lấy được vị trí', this.getGeolocationErrorMessage(error));
        this.cdr.detectChanges();
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  closePopup(): void {
    if (this.popupTimer) {
      clearTimeout(this.popupTimer);
      this.popupTimer = null;
    }
    this.popup = null;
  }

  distanceLabel(branch: Branch): string {
    const distance = this.distances.get(branch.branchId);
    return distance == null ? '' : `${distance.toFixed(distance < 10 ? 1 : 0)} km`;
  }

  branchMapUrl(branch: Branch): string {
    return this.locationService.googleMapsUrl(branch);
  }

  selectBranch(branchId: number): void {
    sessionStorage.setItem('selectedBranchId', String(branchId));
    this.router.navigate(['/services']);
  }

  private setCustomerLocation(latitude: number, longitude: number, address: string): void {
    sessionStorage.setItem('customerAddress', address);
    sessionStorage.setItem('customerLatitude', String(latitude));
    sessionStorage.setItem('customerLongitude', String(longitude));
    this.distances.clear();
    for (const branch of this.branches) {
      if (branch.latitude != null && branch.longitude != null) {
        this.distances.set(branch.branchId, this.locationService.distanceInKm(
          latitude, longitude, branch.latitude, branch.longitude));
      }
    }
  }

  private showPopup(type: 'success' | 'error' | 'info', title: string, message: string): void {
    this.closePopup();
    this.popup = { type, title, message };
    this.popupTimer = setTimeout(() => {
      this.popup = null;
      this.cdr.detectChanges();
    }, 5000);
  }

  private getGeolocationErrorMessage(error: GeolocationPositionError): string {
    if (error.code === error.PERMISSION_DENIED) {
      return 'Bạn cần bật quyền truy cập vị trí cho trình duyệt rồi bấm lại nút Vị trí hiện tại.';
    }
    if (error.code === error.TIMEOUT) {
      return 'Trình duyệt lấy vị trí quá lâu. Hãy kiểm tra GPS/Wi-Fi rồi thử lại.';
    }
    return 'Không thể xác định vị trí hiện tại. Vui lòng thử lại sau.';
  }
}
