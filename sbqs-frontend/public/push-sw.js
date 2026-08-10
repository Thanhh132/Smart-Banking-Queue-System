self.addEventListener('push', (event) => {
  let payload = {};
  try {
    payload = event.data ? event.data.json() : {};
  } catch {
    payload = { title: 'SBQS', body: 'Hàng đợi của bạn vừa được cập nhật.' };
  }

  event.waitUntil(self.registration.showNotification(payload.title || 'SBQS', {
    body: payload.body || 'Hàng đợi của bạn vừa được cập nhật.',
    icon: '/favicon.ico',
    badge: '/favicon.ico',
    tag: payload.tag || 'sbqs-queue-update',
    renotify: true,
    data: { url: payload.url || '/ticket' },
  }));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const targetUrl = new URL(event.notification.data?.url || '/ticket', self.location.origin).href;
  event.waitUntil((async () => {
    const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    const existing = windows.find((client) => client.url.startsWith(self.location.origin));
    if (existing) {
      await existing.focus();
      if ('navigate' in existing) await existing.navigate(targetUrl);
      return;
    }
    await self.clients.openWindow(targetUrl);
  })());
});
