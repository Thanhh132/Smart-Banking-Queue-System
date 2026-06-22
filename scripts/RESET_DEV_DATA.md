# Reset du lieu moi truong dev

Quy trinh nay xoa:

- Toan bo du lieu nghiep vu trong PostgreSQL `db_sbqs`.
- Tat ca user ung dung trong realm Keycloak `SBQS`, ke ca Super Admin.
- Cac sequence ID cua PostgreSQL duoc dua ve gia tri ban dau.

Quy trinh van giu realm `SBQS`, client, role `SUPER_ADMIN`, `BRANCH_ADMIN`, `STAFF`, `CUSTOMER`, va tai khoan Keycloak admin trong realm `master`.

## 1. Dung backend

Dung backend truoc khi reset de tranh request dang ghi du lieu trong luc reset.

## 2. Reset

Chay tai thu muc goc cua project:

```powershell
.\scripts\reset-dev-data.ps1 -Force
```

Script mac dinh su dung:

- Keycloak: `http://localhost:8080`
- Realm: `SBQS`
- Keycloak admin: `admin` / `admin`
- PostgreSQL container: `sbqs-postgres`
- Database: `db_sbqs`

Neu cau hinh dev da thay doi, truyen tham so tuong ung. Vi du:

```powershell
.\scripts\reset-dev-data.ps1 `
  -KeycloakAdminUsername "admin" `
  -KeycloakAdminPassword "mat-khau-admin-keycloak" `
  -Force
```

## 3. Seed lai Super Admin

Trong cung terminal dung de khoi dong backend, dat thong tin tai khoan moi:

```powershell
$env:SBQS_SEED_SUPER_ADMIN_ENABLED="true"
$env:SBQS_SUPER_ADMIN_FULL_NAME="Super Admin"
$env:SBQS_SUPER_ADMIN_EMAIL="superadmin@sbqs.local"
$env:SBQS_SUPER_ADMIN_PASSWORD="ThayBangMatKhauCuaBan123"
$env:SBQS_SUPER_ADMIN_PHONE="0900000000"

cd sbqs-backend
mvn spring-boot:run
```

Seeder se tao cung mot Super Admin trong Keycloak va PostgreSQL. Sau khi log cho biet seed thanh cong, co the dung backend va xoa cac bien moi truong seed:

```powershell
Remove-Item Env:SBQS_SEED_SUPER_ADMIN_ENABLED
Remove-Item Env:SBQS_SUPER_ADMIN_FULL_NAME
Remove-Item Env:SBQS_SUPER_ADMIN_EMAIL
Remove-Item Env:SBQS_SUPER_ADMIN_PASSWORD
Remove-Item Env:SBQS_SUPER_ADMIN_PHONE
```

Sau do khoi dong backend binh thuong. Khong commit mat khau that vao `application.properties`.
