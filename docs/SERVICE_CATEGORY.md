# Danh mục nghiệp vụ dịch vụ

## Quy ước kiến trúc

Trong SBQS, `Services` chính là module **Category Management** theo yêu cầu bài tập. Mỗi bản ghi là một danh mục nghiệp vụ mà khách hàng có thể chọn để lấy số, ví dụ Rút tiền, Chuyển khoản hoặc Mở tài khoản.

`service_catalog` là danh mục mẫu toàn hệ thống do Super Admin quản lý. `services` là dịch vụ đã được một Branch Admin kích hoạt và cấu hình riêng cho chi nhánh. Thuộc tính `serviceType` chỉ dùng để phân nhóm.

## Mô hình dữ liệu

Entity: `sbqs-backend/src/main/java/com/sbqs/entity/Services.java`

Table: `services`

| Trường          | Ý nghĩa                              |
| --------------- | ------------------------------------ |
| `serviceId`     | ID danh mục                          |
| `serviceCode`   | Mã duy nhất trong một chi nhánh      |
| `serviceName`   | Tên danh mục hiển thị cho khách hàng |
| `branch`        | Chi nhánh sở hữu danh mục            |
| `serviceType`   | Nhóm nghiệp vụ                       |
| `description`   | Mô tả dịch vụ                        |
| `estimatedTime` | Thời gian xử lý dự kiến              |
| `status`        | Trạng thái hoạt động                 |

Ràng buộc `(branch_id, service_code)` ngăn trùng mã trong cùng chi nhánh.

## CRUD và phân quyền

Controllers: `ServiceCatalogController` cho danh mục toàn cục và `ServiceController` cho cấu hình theo chi nhánh.

Business service: `sbqs-backend/src/main/java/com/sbqs/service/ServicesService.java`

Repository JPA/Hibernate: `sbqs-backend/src/main/java/com/sbqs/repository/ServiceRepository.java`

| Chức năng | API | Quyền |
| --- | --- | --- |
| Xem danh mục dùng chung | `GET /api/service-catalog` | `SUPER_ADMIN`, `BRANCH_ADMIN` |
| Tạo mẫu dịch vụ toàn hệ thống | `POST /api/service-catalog` | `SUPER_ADMIN` |
| Thêm mẫu vào chi nhánh hiện tại | `POST /api/service-catalog/{id}/add-to-branch` | `BRANCH_ADMIN` |
| Danh sách dịch vụ chi nhánh | `GET /api/services` | `BRANCH_ADMIN`, `CUSTOMER` |
| Cập nhật cấu hình chi nhánh | `PUT /api/services/{id}` | `BRANCH_ADMIN` |
| Xóa khỏi chi nhánh | `DELETE /api/services/{id}` | `BRANCH_ADMIN` |

Quy tắc xóa bảo vệ ticket đang chờ/đang phục vụ và lịch hẹn đang sử dụng dịch vụ. Mapping máy bốc số được dọn trước khi xóa; ticket lịch sử giữ nguyên nhưng bỏ liên kết trực tiếp tới bản ghi đã xóa.

Phân quyền API được cấu hình tại `sbqs-backend/src/main/java/com/sbqs/config/SecurityConfig.java`.

## Giao diện quản lý

Trang CRUD: `sbqs-frontend/src/app/pages/admin/services/`

API client: `sbqs-frontend/src/app/core/services/admin-services.service.ts`

Super Admin tạo mẫu tại `/super-admin/services`. Branch Admin chọn mẫu và cấu hình riêng tại `/admin/services`; cả hai route đều được bảo vệ bằng `roleGuard`.

Khách hàng chỉ thấy dịch vụ đang hoạt động và đã được mapping với máy bốc số tại trang chọn dịch vụ.

## Import Excel

Endpoint: `POST /api/import/services`

Template: `GET /api/import/templates/services`

Backend: `sbqs-backend/src/main/java/com/sbqs/service/BulkImportService.java`

Frontend dùng component chung: `sbqs-frontend/src/app/shared/components/excel-import-panel/`.

Import kiểm tra định dạng file, dữ liệu bắt buộc, mã trùng và chỉ chấp nhận mã đã tồn tại trong danh mục dùng chung.

## Quan hệ trong nghiệp vụ hàng đợi

- `queue_machine_services`: xác định danh mục nào được cung cấp tại từng máy bốc số.
- `tickets.service_id`: lưu dịch vụ khách hàng đã chọn.
- `appointments.service_id`: lưu dịch vụ của lịch hẹn.
- Redis cache `services`: giảm truy vấn danh sách dịch vụ theo chi nhánh.
- Kafka domain events: phát sự kiện tạo, sửa và xóa dịch vụ.

Vì vậy `Services` đáp ứng đầy đủ vai trò category management cả frontend, backend và database mà không cần thêm một module dữ liệu trùng lặp.
