import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';

interface PushPublicKeyResponse {
  enabled: boolean;
  publicKey: string;
}

interface PushSubscriptionPayload {
  endpoint: string;
  p256dh: string;
  auth: string;
}

@Injectable({ providedIn: 'root' })
export class WebPushService {
  private http = inject(HttpClient);
  private apiUrl = `${inject(API_BASE_URL)}/push`;
  private registration: ServiceWorkerRegistration | null = null;
  private publicKey = '';

  readonly supported = signal(false);
  readonly serverEnabled = signal(false);
  readonly subscribed = signal(false);
  readonly busy = signal(false);
  readonly permission = signal<NotificationPermission>(
    typeof Notification === 'undefined' ? 'default' : Notification.permission,
  );
  readonly message = signal('');

  async initialize(): Promise<void> {
    this.supported.set(this.isSupported());
    if (!this.supported()) return;

    try {
      const config = await firstValueFrom(
        this.http.get<PushPublicKeyResponse>(`${this.apiUrl}/public-key`),
      );
      this.serverEnabled.set(config.enabled);
      this.publicKey = config.publicKey || '';
      if (!config.enabled) return;

      this.registration = await navigator.serviceWorker.register('/push-sw.js', { scope: '/' });
      const existing = await this.registration.pushManager.getSubscription();
      this.subscribed.set(!!existing);
      this.permission.set(Notification.permission);
      if (existing) await this.saveSubscription(existing);
    } catch {
      this.message.set('Không thể khởi tạo thông báo trên thiết bị này.');
    }
  }

  async enable(): Promise<void> {
    if (!this.supported() || !this.serverEnabled() || this.busy()) return;
    this.busy.set(true);
    this.message.set('');
    try {
      const permission = await Notification.requestPermission();
      this.permission.set(permission);
      if (permission !== 'granted') {
        this.message.set('Bạn chưa cho phép SBQS gửi thông báo trên điện thoại.');
        return;
      }
      this.registration ??= await navigator.serviceWorker.register('/push-sw.js', { scope: '/' });
      const existing = await this.registration.pushManager.getSubscription();
      const subscription = existing ?? await this.registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: this.urlBase64ToUint8Array(this.publicKey),
      });
      await this.saveSubscription(subscription);
      this.subscribed.set(true);
      this.message.set('Đã bật nhắc khi còn 3 người và khi đến lượt.');
    } catch {
      this.message.set('Không bật được thông báo. Hãy kiểm tra quyền thông báo của trình duyệt.');
    } finally {
      this.busy.set(false);
    }
  }

  async disable(): Promise<void> {
    if (!this.registration || this.busy()) return;
    this.busy.set(true);
    this.message.set('');
    try {
      const subscription = await this.registration.pushManager.getSubscription();
      if (subscription) {
        await firstValueFrom(
          this.http.delete(`${this.apiUrl}/subscriptions`, { body: this.toPayload(subscription) }),
        );
        await subscription.unsubscribe();
      }
      this.subscribed.set(false);
      this.message.set('Đã tắt thông báo hàng đợi trên thiết bị này.');
    } catch {
      this.message.set('Không tắt được thông báo. Vui lòng thử lại.');
    } finally {
      this.busy.set(false);
    }
  }

  private async saveSubscription(subscription: PushSubscription): Promise<void> {
    await firstValueFrom(
      this.http.post(`${this.apiUrl}/subscriptions`, this.toPayload(subscription)),
    );
  }

  private toPayload(subscription: PushSubscription): PushSubscriptionPayload {
    const json = subscription.toJSON();
    if (!json.endpoint || !json.keys?.['p256dh'] || !json.keys?.['auth']) {
      throw new Error('PushSubscription is incomplete');
    }
    return { endpoint: json.endpoint, p256dh: json.keys['p256dh'], auth: json.keys['auth'] };
  }

  private isSupported(): boolean {
    return typeof window !== 'undefined'
      && window.isSecureContext
      && 'serviceWorker' in navigator
      && 'PushManager' in window
      && 'Notification' in window;
  }

  private urlBase64ToUint8Array(value: string): Uint8Array<ArrayBuffer> {
    const padding = '='.repeat((4 - value.length % 4) % 4);
    const base64 = (value + padding).replace(/-/g, '+').replace(/_/g, '/');
    const raw = atob(base64);
    const bytes = new Uint8Array(new ArrayBuffer(raw.length));
    for (let index = 0; index < raw.length; index++) bytes[index] = raw.charCodeAt(index);
    return bytes;
  }
}
