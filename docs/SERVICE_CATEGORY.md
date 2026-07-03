# Danh mục nghiệp vụ dịch vụ

## Quy ước kiến trúc

Trong SBQS, `Services` chính là module **Category Management** theo yêu cầu bài tập. Mỗi bản ghi là một danh mục nghiệp vụ mà khách hàng có thể chọn để lấy số, ví dụ Rút tiền, Chuyển khoản hoặc Mở tài khoản.

Không tạo thêm bảng `categories` vì bảng đó sẽ trùng dữ liệu và quan hệ với `services`. Thuộc tính `serviceType` dùng để phân nhóm danh mục dịch vụ, không phải một danh mục độc lập.

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

Controller: `sbqs-backend/src/main/java/com/sbqs/controller/ServiceController.java`

Business service: `sbqs-backend/src/main/java/com/sbqs/service/ServicesService.java`

Repository JPA/Hibernate: `sbqs-backend/src/main/java/com/sbqs/repository/ServiceRepository.java`

| Chức năng         | API                         | Quyền                      |
| ----------------- | --------------------------- | -------------------------- |
| Danh sách và lọc  | `GET /api/services`         | `BRANCH_ADMIN`, `CUSTOMER` |
| Tạo danh mục      | `POST /api/services`        | `BRANCH_ADMIN`             |
| Cập nhật danh mục | `PUT /api/services/{id}`    | `BRANCH_ADMIN`             |
| Xóa danh mục      | `DELETE /api/services/{id}` | `BRANCH_ADMIN`             |

Quy tắc xóa bảo vệ ticket đang chờ/đang phục vụ và lịch hẹn đang sử dụng dịch vụ. Mapping máy bốc số được dọn trước khi xóa; ticket lịch sử giữ nguyên nhưng bỏ liên kết trực tiếp tới bản ghi đã xóa.

Phân quyền API được cấu hình tại `sbqs-backend/src/main/java/com/sbqs/config/SecurityConfig.java`.

## Giao diện quản lý

Trang CRUD: `sbqs-frontend/src/app/pages/admin/services/`

API client: `sbqs-frontend/src/app/core/services/admin-services.service.ts`

Route: `/admin/services`, chỉ dành cho `BRANCH_ADMIN` qua `roleGuard` trong `sbqs-frontend/src/app/app.routes.ts`.

Khách hàng chỉ thấy dịch vụ đang hoạt động và đã được mapping với máy bốc số tại trang chọn dịch vụ.

## Import Excel

Endpoint: `POST /api/import/services`

Template: `GET /api/import/templates/services`

Backend: `sbqs-backend/src/main/java/com/sbqs/service/BulkImportService.java`

Frontend dùng component chung: `sbqs-frontend/src/app/shared/components/excel-import-panel/`.

Import kiểm tra định dạng file, dữ liệu bắt buộc, mã trùng trong file và mã đã tồn tại trong chi nhánh trước khi tạo hàng loạt.

## Quan hệ trong nghiệp vụ hàng đợi

- `queue_machine_services`: xác định danh mục nào được cung cấp tại từng máy bốc số.
- `tickets.service_id`: lưu dịch vụ khách hàng đã chọn.
- `appointments.service_id`: lưu dịch vụ của lịch hẹn.
- Redis cache `services`: giảm truy vấn danh sách dịch vụ theo chi nhánh.
- Kafka domain events: phát sự kiện tạo, sửa và xóa dịch vụ.

Vì vậy `Services` đáp ứng đầy đủ vai trò category management cả frontend, backend và database mà không cần thêm một module dữ liệu trùng lặp.
