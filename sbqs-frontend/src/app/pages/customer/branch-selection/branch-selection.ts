import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { Branch, SmartBranchRecommendation } from '../../../core/models/branch.model';
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
export class BranchSelection implements OnInit, OnDestroy {
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
  isRouting = false;
  routingError = '';
  private distances = new Map<number, number>();
  private recommendations = new Map<number, SmartBranchRecommendation>();
  private popupTimer: ReturnType<typeof setTimeout> | null = null;
  private routingTimer: ReturnType<typeof setInterval> | null = null;

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
      .sort((first, second) => {
        const firstRoutingScore = this.recommendations.get(first.branchId)?.routingScore;
        const secondRoutingScore = this.recommendations.get(second.branchId)?.routingScore;
        if (firstRoutingScore != null || secondRoutingScore != null) {
          return (firstRoutingScore ?? Number.MAX_VALUE) - (secondRoutingScore ?? Number.MAX_VALUE);
        }
        return (this.distances.get(first.branchId) ?? Number.MAX_VALUE)
          - (this.distances.get(second.branchId) ?? Number.MAX_VALUE);
      });
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

  ngOnDestroy(): void {
    if (this.popupTimer) {
      clearTimeout(this.popupTimer);
    }
    if (this.routingTimer) {
      clearInterval(this.routingTimer);
    }
  }

  /** Xin vị trí trình duyệt, reverse-geocode địa chỉ và tính khoảng cách tới từng chi nhánh. */
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
    const distance = this.recommendations.get(branch.branchId)?.distanceKm
      ?? this.distances.get(branch.branchId);
    return distance == null ? '' : `${distance.toFixed(distance < 10 ? 1 : 0)} km`;
  }

  recommendationFor(branchId: number): SmartBranchRecommendation | undefined {
    return this.recommendations.get(branchId);
  }

  onBankChange(): void {
    this.recommendations.clear();
    this.loadSmartRecommendations();
  }

  branchMapUrl(branch: Branch): string {
    return this.locationService.googleMapsUrl(branch);
  }

  /** Lưu chi nhánh cho toàn bộ các bước chọn dịch vụ, cấp số và theo dõi phía sau. */
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
    this.loadSmartRecommendations();
    this.startRoutingRefresh();
  }

  /** Refresh smart-routing load every 15 seconds after the customer shares a location. */
  private loadSmartRecommendations(): void {
    const latitudeValue = sessionStorage.getItem('customerLatitude');
    const longitudeValue = sessionStorage.getItem('customerLongitude');
    const latitude = Number(latitudeValue);
    const longitude = Number(longitudeValue);
    if (!latitudeValue || !longitudeValue || !Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      return;
    }

    this.isRouting = true;
    this.routingError = '';
    this.branchService.getSmartRecommendations(this.selectedBank, latitude, longitude).subscribe({
      next: (data) => {
        this.recommendations = new Map((data || []).map((item) => [item.branchId, item]));
        this.isRouting = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isRouting = false;
        this.routingError = 'Chưa cập nhật được tải hàng đợi. Danh sách vẫn được sắp theo khoảng cách.';
        this.cdr.detectChanges();
      },
    });
  }

  private startRoutingRefresh(): void {
    if (this.routingTimer) {
      clearInterval(this.routingTimer);
    }
    this.routingTimer = setInterval(() => this.loadSmartRecommendations(), 15000);
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
