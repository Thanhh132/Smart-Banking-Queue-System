import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';

import { Branch, SmartBranchRecommendation } from '../../../core/models/branch.model';
import { BranchService } from '../../../core/services/branch.service';
import { LocationService } from '../../../core/services/location.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';
import { AppIcon } from '../../../shared/components/app-icon/app-icon';
import { AppButton } from '../../../shared/components/app-button/app-button';
import { AppEmptyState } from '../../../shared/components/app-empty-state/app-empty-state';
import { AppLoadingState } from '../../../shared/components/app-loading-state/app-loading-state';
import { AppPageHeader } from '../../../shared/components/app-page-header/app-page-header';
import { AppStatusBadge } from '../../../shared/components/app-status-badge/app-status-badge';

@Component({
  selector: 'app-branch-selection',
  imports: [CommonModule, FormsModule, DashboardLayout, AppIcon, AppButton, AppEmptyState, AppLoadingState, AppPageHeader, AppStatusBadge],
  templateUrl: './branch-selection.html',
  styleUrl: './branch-selection.scss',
})
export class BranchSelection implements OnInit, OnDestroy {
  private branchService = inject(BranchService);
  private locationService = inject(LocationService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  branches: Branch[] = [];
  popup: { type: 'success' | 'error' | 'info'; title: string; message: string } | null = null;
  searchTerm = '';
  selectedBank = 'ALL';
  customerLocationLabel = sessionStorage.getItem('customerAddress') || '';
  isLocating = false;
  isRouting = false;
  routingError = '';
  selectingBranchId: number | null = null;
  private distances = new Map<number, number>();
  private recommendations = new Map<number, SmartBranchRecommendation>();
  private openStatuses = new Map<number, boolean>();
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
    if (this.route.snapshot.queryParamMap.has('closedBranch')) {
      this.showPopup(
        'info',
        'Chi nhánh đã đóng cửa',
        'Bạn không thể vào bước lấy số ngoài giờ phục vụ. Vui lòng quay lại trong khung giờ làm việc.',
      );
    } else if (this.route.snapshot.queryParamMap.has('scheduleUnavailable')) {
      this.showPopup(
        'error',
        'Chưa kiểm tra được giờ phục vụ',
        'Hệ thống tạm thời không cho lấy số để tránh cấp phiếu khi chi nhánh đã đóng cửa.',
      );
    }

    this.branchService.getBranches().subscribe({
      next: (data) => {
        this.branches = (data || []).filter((branch) => branch.status === 'ACTIVE');
        this.loadOpenStatuses();
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

  isOpenStatusLoaded(branchId: number): boolean {
    return this.openStatuses.has(branchId);
  }

  isBranchOpen(branchId: number): boolean {
    return this.openStatuses.get(branchId) === true;
  }

  get routingStatusLabel(): string {
    if (this.isRouting) return 'Đang cập nhật tải...';
    if (this.routingError) return 'Chưa có đề xuất';
    return this.recommendations.size > 0 ? 'Đề xuất theo tải trực tiếp' : 'Chưa chia sẻ vị trí';
  }

  onBankChange(): void {
    this.recommendations.clear();
    this.loadSmartRecommendations();
  }

  branchMapUrl(branch: Branch): string {
    return this.locationService.googleMapsUrl(branch);
  }

  /** Lưu chi nhánh cho toàn bộ các bước chọn dịch vụ, cấp số và theo dõi phía sau. */
  selectBranch(branch: Branch): void {
    if (!this.isOpenStatusLoaded(branch.branchId)) {
      this.showPopup('info', 'Đang kiểm tra giờ phục vụ', 'Vui lòng chờ trong giây lát rồi thử lại.');
      return;
    }
    if (!this.isBranchOpen(branch.branchId)) {
      this.showPopup(
        'info',
        'Chi nhánh đã đóng cửa',
        'Chi nhánh hiện ngoài giờ phục vụ nên chưa thể cấp số.',
      );
      return;
    }

    this.selectingBranchId = branch.branchId;
    this.branchService.getOpenStatus(branch.branchId).subscribe({
      next: (status) => {
        this.openStatuses.set(branch.branchId, status.openNow);
        this.selectingBranchId = null;
        if (!status.openNow) {
          this.showPopup('info', 'Chi nhánh vừa đóng cửa', status.message);
          this.cdr.detectChanges();
          return;
        }
        sessionStorage.setItem('selectedBranchId', String(branch.branchId));
        this.router.navigate(['/services']);
      },
      error: () => {
        this.selectingBranchId = null;
        this.showPopup(
          'error',
          'Chưa kiểm tra được giờ phục vụ',
          'Hệ thống chưa thể xác nhận chi nhánh đang mở nên không cấp số lúc này.',
        );
        this.cdr.detectChanges();
      },
    });
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
        const items = data || [];
        this.recommendations = new Map(items.map((item) => [item.branchId, item]));
        if (items.length === 0) {
          this.routingError = 'Hiện không có chi nhánh nào đang mở để đề xuất. Bạn vẫn có thể xem danh sách, nhưng chỉ lấy số trong giờ phục vụ.';
        } else if (!items.some((item) => item.recommended)) {
          this.routingError = 'Các chi nhánh đang mở hiện chưa có quầy hoạt động nên hệ thống chưa thể chọn đề xuất tối ưu.';
        }
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
    this.routingTimer = setInterval(() => {
      this.loadSmartRecommendations();
      this.loadOpenStatuses();
    }, 15000);
  }

  private loadOpenStatuses(): void {
    if (this.branches.length === 0) {
      this.openStatuses.clear();
      return;
    }

    forkJoin(
      this.branches.map((branch) =>
        this.branchService.getOpenStatus(branch.branchId).pipe(
          catchError(() => of({
            branchId: branch.branchId,
            openNow: false,
            message: 'Chưa kiểm tra được giờ phục vụ',
            checkedAt: new Date().toISOString(),
          })),
        ),
      ),
    ).subscribe((statuses) => {
      this.openStatuses = new Map(statuses.map((status) => [status.branchId, status.openNow]));
      this.cdr.detectChanges();
    });
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
