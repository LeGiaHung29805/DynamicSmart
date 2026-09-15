# NỀN XÁC THỰC VÀ JWT — DYNAMICMART

> Chủ sở hữu: trưởng nhóm/phần nền chung. Tiến dùng contract này cho giao diện đăng nhập; Thảo dùng principal/role để kiểm tra ownership cho Profile, Address, Cart và Voucher.

## 1. Mục tiêu P0

- Đăng ký Customer, đăng nhập, đăng xuất và làm mới phiên.
- Access token JWT ngắn hạn (mặc định 15 phút), gửi trong response body để frontend giữ trong bộ nhớ.
- Refresh token ngẫu nhiên chỉ ở cookie `HttpOnly`, lưu **hash SHA-256** trong `identity_db`, có rotation và phát hiện reuse theo token family.
- Gateway xác minh chữ ký HMAC-SHA256, issuer và expiry trước khi route yêu cầu bảo vệ.
- Access token có `sub` (user ID), `role`, `authVersion`, `iss`, `iat`, `exp`, `jti`.

## 2. Endpoint hiện có

| Endpoint | Quyền | Kết quả |
|---|---|---|
| `POST /api/v1/auth/register` | Public | Tạo `CUSTOMER`, trả access token và set refresh cookie |
| `POST /api/v1/auth/login` | Public | Kiểm tra mật khẩu BCrypt, trả access token và set refresh cookie |
| `POST /api/v1/auth/refresh` | Cookie refresh | Rotate refresh token, trả access token mới/set cookie mới |
| `POST /api/v1/auth/logout` | Cookie refresh tùy chọn | Revoke token hiện có, xóa cookie |

Mọi endpoint khác đi qua Gateway yêu cầu `Authorization: Bearer <access-token>`, trừ endpoint public đã whitelist.

## 3. Quy tắc bảo mật

- Không commit `JWT_HMAC_SECRET_BASE64`. Identity và Gateway trong **cùng môi trường** phải dùng cùng secret Base64 giải mã tối thiểu 32 byte.
- Development local dùng `AUTH_REFRESH_COOKIE_SECURE=false`; production bắt buộc `true` và HTTPS.
- Cookie path là `/api/v1/auth`, `HttpOnly`, `SameSite=Strict`; trình duyệt không đọc được refresh token.
- `refresh_tokens.token_hash` là hash, không có refresh token thô trong database/log/API response.
- Dùng refresh token cũ sau rotation sẽ revoke toàn bộ token family hiện tại và buộc login lại.
- `role` chỉ là claim để Gateway/UI thuận tiện. Mỗi business service vẫn kiểm tra ownership/role tại backend, không tin customer ID/role do frontend gửi.
- `authVersion` đã được phát trong claim. Việc invalidation access token ngay lập tức khi Admin khóa/đổi role cần cache/event verifier ở Gateway; P0 dựa thêm vào access token TTL ngắn và revoke refresh token. Không được tuyên bố logout toàn bộ access token tức thì khi chưa làm verifier này.

## 4. Cấu hình local trong IntelliJ

Cho `identity-service`:

```text
SPRING_PROFILES_ACTIVE=local
DB_URL=jdbc:postgresql://localhost:5432/identity_db
DB_USERNAME=...
DB_PASSWORD=...
JWT_ISSUER=dynamicmart-identity-service
JWT_HMAC_SECRET_BASE64=...
AUTH_REFRESH_COOKIE_SECURE=false
```

Cho `api-gateway`, giữ **cùng** `JWT_ISSUER` và `JWT_HMAC_SECRET_BASE64`.

### Tài khoản demo local

Khi chạy Identity với profile `local`, Flyway chạy thêm `db/seed/local/V2__seed_demo_users.sql`. Ba tài khoản dưới đây chỉ để kiểm thử trên máy cá nhân; migration production không nạp seed này.

| Email | Vai trò | Mật khẩu |
|---|---|---|
| `admin@dynamicmart.local` | Admin | `Password@123` |
| `customer@dynamicmart.local` | Customer | `Password@123` |
| `customer2@dynamicmart.local` | Customer | `Password@123` |

Database `identity_db` phải được tạo trước trong PostgreSQL. Chạy script `infra/postgres/00_create_local_databases.sql` khi khởi tạo local; script chỉ tạo database thiếu. Không đặt `CREATE DATABASE` trong Flyway migration vì Flyway chỉ chạy sau khi đã kết nối đến database đó.

Tạo secret local bằng PowerShell (chỉ dùng local, không commit output):

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

## 5. Phần chưa làm

- Tiến phụ trách giao diện quên/đặt lại mật khẩu. Trang chờ đã có ở frontend, nhưng API reset và email giao dịch chưa được làm.
- Endpoint `me`, logout mọi thiết bị, xác minh email và rate limit login/reset.
- Gateway verifier cho `authVersion`, JWK/RSA key rotation cho production.
- CORS production allow-list và rate limit theo IP/user.

Các phần này phải được bổ sung theo migration/contract mới, không sửa `V1__initial_schema.sql` sau khi môi trường chung đã chạy Flyway.
