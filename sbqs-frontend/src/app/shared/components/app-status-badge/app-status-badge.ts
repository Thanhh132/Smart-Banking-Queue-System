import { Component, computed, input } from '@angular/core';

type BadgeTone = 'primary' | 'secondary' | 'success' | 'warning' | 'danger' | 'info';

const STATUS_META: Record<string, { label: string; tone: BadgeTone }> = {
  ACTIVE: { label: 'Hoạt động', tone: 'success' },
  AVAILABLE: { label: 'Sẵn sàng', tone: 'success' },
  OPEN: { label: 'Đang mở', tone: 'success' },
  INACTIVE: { label: 'Ngừng hoạt động', tone: 'secondary' },
  CLOSED: { label: 'Đã đóng', tone: 'secondary' },
  IDLE: { label: 'Sẵn sàng', tone: 'info' },
  WAITING: { label: 'Đang chờ', tone: 'warning' },
  PENDING: { label: 'Chờ xử lý', tone: 'warning' },
  SERVING: { label: 'Đang phục vụ', tone: 'primary' },
  COMPLETED: { label: 'Hoàn thành', tone: 'success' },
  CANCELLED: { label: 'Đã hủy', tone: 'danger' },
  MISSED: { label: 'Không đến', tone: 'danger' },
  ERROR: { label: 'Lỗi', tone: 'danger' },
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  templateUrl: './app-status-badge.html',
  styleUrl: './app-status-badge.scss',
})
export class AppStatusBadge {
  readonly status = input.required<string>();
  readonly label = input('');

  readonly normalizedStatus = computed(() => this.status().trim().toUpperCase());
  readonly meta = computed(() => STATUS_META[this.normalizedStatus()] ?? {
    label: this.status(),
    tone: 'secondary' as BadgeTone,
  });
  readonly displayLabel = computed(() => this.label() || this.meta().label);
  readonly toneClass = computed(() => `app-status-badge--${this.meta().tone}`);
}
