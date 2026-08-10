# Đăng nhập Google qua Keycloak

SBQS dùng Keycloak làm Identity Broker. Google chỉ xác thực email; hồ sơ giao dịch trong SBQS vẫn yêu cầu người dùng tự nhập họ tên theo giấy tờ, số điện thoại, địa chỉ thường trú và địa chỉ hiện tại.

## 1. Tạo OAuth Client trên Google Cloud

Tạo OAuth 2.0 Client loại **Web application** và khai báo Authorized redirect URI:

```text
http://localhost:8080/realms/SBQS/broker/google/endpoint
```

Đây là callback từ Google về Keycloak, không phải callback Angular.

## 2. Cấp credentials cho Keycloak

Không ghi Client ID hoặc Client Secret vào Git. Trước khi tạo lại Keycloak lần đầu, đặt biến môi trường:

```powershell
$env:GOOGLE_CLIENT_ID="<google-client-id>"
$env:GOOGLE_CLIENT_SECRET="<google-client-secret>"
docker compose up -d --force-recreate keycloak-iam
```

Realm import chỉ chạy khi realm `SBQS` chưa tồn tại. Nếu đang dùng database Keycloak hiện có, vào `http://localhost:8080/admin`, chọn realm `SBQS`, sau đó:

1. `Identity providers` → `Google`.
2. Nhập Google Client ID và Client Secret, bật `Trust email`, lưu lại.
3. Giữ alias là `google`.
4. Đặt `Update profile on first login` thành `Off`. Hồ sơ pháp lý/giao dịch được SBQS thu thập tại `/complete-profile`, không dùng tên Google làm họ tên thật.
5. Đặt `First login flow override` thành `google first broker login`. Flow này vô hiệu hóa hoàn toàn bước `Review Profile` của Keycloak.
6. Trong `Authentication` → `Required actions`, tắt `Verify Profile`. SBQS có màn hình `/complete-profile` riêng và không dùng hồ sơ Keycloak làm hồ sơ giao dịch.
7. Trong client `sbqs-frontend`, bảo đảm Standard Flow bật và redirect URI có `http://localhost:4200/*`.
8. Mapper `User Session Note` cho `identity_provider` là tùy chọn để token dễ quan sát hơn. Backend vẫn kiểm tra Federated Identity trực tiếp qua Keycloak nếu claim này không có.

## 3. Cấu hình ứng dụng

Các giá trị mặc định phục vụ local development:

```text
KEYCLOAK_PUBLIC_URL=http://localhost:8080
SBQS_GOOGLE_REDIRECT_URI=http://localhost:4200/auth/google/callback
SBQS_GOOGLE_LOGIN_ENABLED=true
```

Ở môi trường triển khai, thay cả URL Keycloak và callback Angular bằng HTTPS. Callback Angular phải nằm trong redirect URI hợp lệ của client Keycloak.

## Luồng nghiệp vụ

1. Angular tạo `state`, PKCE verifier và SHA-256 challenge, sau đó mở cửa sổ chọn tài khoản Google. Tham số `kc_idp_hint=google` bỏ qua màn hình đăng nhập Keycloak.
2. Keycloak xác thực Google ở phía sau. Nếu Google identity đã liên kết thì đăng nhập ngay; nếu chưa có tài khoản và email là duy nhất thì tự tạo người dùng Keycloak, không hiện trang nhập hồ sơ của Keycloak.
3. Callback Angular chỉ gửi authorization code về cửa sổ SBQS đã mở nó. Cửa sổ chính kiểm tra đúng `origin`, `state`, PKCE rồi gọi backend đổi code lấy token.
4. Backend xác minh Google qua claim hoặc Federated Identity của Keycloak, chỉ cho phép role `CUSTOMER` và đồng bộ tài khoản ứng dụng.
5. Tài khoản chưa có đủ hồ sơ bị chuyển đến `/complete-profile`; lấy số và tạo ủy quyền chỉ khả dụng sau khi hồ sơ hoàn tất.

Nếu trình duyệt chặn popup, Angular tự chuyển sang luồng đổi toàn bộ trang. Khi Google từ chối, popup bị đóng, callback sai hoặc đổi code thất bại, SBQS xóa phiên local để không tái sử dụng nhầm token của lần đăng nhập trước.

Nếu email đã thuộc một tài khoản Keycloak cũ nhưng chưa liên kết Google, flow mặc định yêu cầu xác minh/liên kết một lần. Không bật `Automatically Set Existing User`: tự động liên kết chỉ dựa trên email có thể ghép nhầm Google với tài khoản quản trị. Tài khoản `STAFF`, `BRANCH_ADMIN` và `SUPER_ADMIN` tiếp tục dùng luồng đăng nhập được quản trị cấp.
