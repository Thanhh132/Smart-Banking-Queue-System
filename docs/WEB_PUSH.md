# Web Push queue notifications

SBQS can notify a subscribed customer device when exactly three tickets remain ahead and when staff calls the ticket. Notifications are delivered by the browser push service, so the customer does not need to keep the SBQS page open.

## Local configuration

Generate one VAPID key pair for an environment. For example, the commonly used `web-push` CLI can generate a compatible URL-safe key pair:

```powershell
npx --yes web-push generate-vapid-keys
```

Set the generated values as environment variables before starting the backend:

```powershell
$env:SBQS_WEB_PUSH_PUBLIC_KEY="<public-key>"
$env:SBQS_WEB_PUSH_PRIVATE_KEY="<private-key>"
$env:SBQS_WEB_PUSH_SUBJECT="mailto:admin@example.com"
cd sbqs-backend
.\mvnw.cmd spring-boot:run
```

Do not commit the private key to `application.properties`, `application-local.properties`, `.env`, screenshots, logs, or documentation. Keep the same key pair while subscriptions are active; replacing it requires customers to subscribe again.

## Browser behavior

- Android/desktop Chrome, Edge, and Firefox can subscribe from the ticket page after the customer presses **Bật thông báo**.
- On iPhone/iPad, add SBQS to the Home Screen and enable notifications from the installed web app.
- Production must use HTTPS. `localhost` is treated as a secure context for local development.
- Notification permission is controlled by the customer and the operating system. If permission is denied, SBQS cannot turn it back on programmatically.

## Delivery flow

1. The browser creates a `PushSubscription`; the authenticated customer registers it with `/api/push/subscriptions`.
2. Calling the next ticket publishes an after-commit event for the new serving ticket and, when applicable, the fourth remaining ticket in that queue.
3. The async notification listener sends Web Push outside the ticket transaction.
4. `web_push_deliveries` prevents duplicate notifications for the same ticket, device, and notification type. Expired subscriptions are disabled automatically.

Kafka receives best-effort ticket domain events for integration/observability, but Web Push delivery does not depend on a Kafka consumer.
