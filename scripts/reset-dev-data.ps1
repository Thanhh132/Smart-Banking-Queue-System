[CmdletBinding()]
param(
    [string]$KeycloakUrl = "http://localhost:8080",
    [string]$Realm = "SBQS",
    [string]$KeycloakAdminUsername = "admin",
    [string]$KeycloakAdminPassword = "admin",
    [string]$PostgresContainer = "sbqs-postgres",
    [string]$DatabaseName = "db_sbqs",
    [string]$DatabaseUsername = "admin",
    [switch]$Force
)

$ErrorActionPreference = "Stop"

if (-not $Force) {
    throw @"
Thao tac nay se xoa TOAN BO du lieu nghiep vu trong '$DatabaseName' va tat ca user thuong trong realm '$Realm'.
Realm, client, role va tai khoan Keycloak admin o realm master se duoc giu lai.

Chay lai voi -Force neu ban chac chan:
  .\scripts\reset-dev-data.ps1 -Force
"@
}

Write-Host "[Kiem tra] Dang kiem tra PostgreSQL container..."
& docker exec $PostgresContainer `
    psql `
    -v ON_ERROR_STOP=1 `
    -U $DatabaseUsername `
    -d $DatabaseName `
    -At `
    -c "SELECT 1;"

if ($LASTEXITCODE -ne 0) {
    throw "Khong ket noi duoc PostgreSQL. Kiem tra Docker va container '$PostgresContainer'. Chua co du lieu nao bi xoa."
}

function Invoke-KeycloakRequest {
    param(
        [Parameter(Mandatory)]
        [string]$Uri,
        [Parameter(Mandatory)]
        [ValidateSet("GET", "POST", "DELETE")]
        [string]$Method,
        [hashtable]$Headers,
        [hashtable]$Body,
        [string]$ContentType
    )

    $parameters = @{
        Uri = $Uri
        Method = $Method
    }

    if ($Headers) {
        $parameters.Headers = $Headers
    }
    if ($Body) {
        $parameters.Body = $Body
    }
    if ($ContentType) {
        $parameters.ContentType = $ContentType
    }

    Invoke-RestMethod @parameters
}

Write-Host "[1/3] Dang kiem tra quyen quan tri Keycloak..."
$token = Invoke-KeycloakRequest `
    -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" `
    -Method POST `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        grant_type = "password"
        client_id = "admin-cli"
        username = $KeycloakAdminUsername
        password = $KeycloakAdminPassword
    }

if (-not $token.access_token) {
    throw "Keycloak khong tra ve admin access token."
}

$headers = @{ Authorization = "Bearer $($token.access_token)" }
$realmUsers = @(
    Invoke-KeycloakRequest `
        -Uri "$KeycloakUrl/admin/realms/$Realm/users?first=0&max=10000" `
        -Method GET `
        -Headers $headers
)

$applicationUsers = @(
    $realmUsers | Where-Object {
        $_.id -and ([string]$_.username -notlike "service-account-*")
    }
)

Write-Host "[2/3] Dang xoa $($applicationUsers.Count) user ung dung khoi realm '$Realm'..."
foreach ($user in $applicationUsers) {
    Invoke-KeycloakRequest `
        -Uri "$KeycloakUrl/admin/realms/$Realm/users/$($user.id)" `
        -Method DELETE `
        -Headers $headers
    Write-Host "      Da xoa: $($user.username)"
}

$truncateSql = @'
DO $reset$
DECLARE
    tables_to_truncate text;
BEGIN
    SELECT string_agg(format('%I.%I', schemaname, tablename), ', ')
    INTO tables_to_truncate
    FROM pg_tables
    WHERE schemaname = 'public';

    IF tables_to_truncate IS NOT NULL THEN
        EXECUTE 'TRUNCATE TABLE ' || tables_to_truncate || ' RESTART IDENTITY CASCADE';
    END IF;
END
$reset$;
'@

Write-Host "[3/3] Dang xoa du lieu nghiep vu va dat lai cac bo dem ID..."
& docker exec $PostgresContainer `
    psql `
    -v ON_ERROR_STOP=1 `
    -U $DatabaseUsername `
    -d $DatabaseName `
    -c $truncateSql

if ($LASTEXITCODE -ne 0) {
    throw "Khong the reset PostgreSQL. Kiem tra Docker va container '$PostgresContainer'."
}

Write-Host ""
Write-Host "Reset thanh cong. Realm, client va role Keycloak van duoc giu nguyen." -ForegroundColor Green
Write-Host "Hay seed lai Super Admin khi khoi dong backend bang cac bien moi truong duoc huong dan trong scripts/RESET_DEV_DATA.md."
