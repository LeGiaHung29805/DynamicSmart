# PostgreSQL và `.env` khi chạy backend local

Mỗi Spring Boot service đọc file `.env` qua `spring.config.import` ở `src/main/resources/application.yaml`. YAML thử các vị trí tương ứng khi chạy từ thư mục service, thư mục `services`, hoặc thư mục gốc repository. File `.env` dùng cú pháp `KEY=value` (như `.env.example`), được nạp dưới dạng Java properties. `SPRING_PROFILES_ACTIVE` được ánh xạ thành `spring.profiles.active` trong YAML để profile `local` có hiệu lực. Không commit `.env` vì file này chứa mật khẩu và các secret.

Mỗi service cần có `.env` riêng và thay các giá trị mẫu bằng thông tin thật trước khi chạy. Ví dụ trong PowerShell:

```powershell
cd E:\dynamicmart\services\order-service
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
# Chỉnh DB_USERNAME, DB_PASSWORD và các biến cần thiết trong .env
.\mvnw.cmd spring-boot:run
```

Có thể chạy Maven/JAR từ thư mục service hoặc từ thư mục gốc repository. Nếu IDE đặt Working directory ở một thư mục khác, hãy đặt lại về một trong hai vị trí này. Trong Docker hoặc môi trường triển khai, truyền biến môi trường vào process/container; file `.env` local không bắt buộc vì import được khai báo `optional:`.

Database PostgreSQL cần tương ứng với `DB_URL` trong từng `.env`: `identity_db`, `catalog_db`, `cart_db`, `order_db`, `payment_db`, `engagement_db`. PostgreSQL phải chạy trên host/port được khai báo (mặc định local là `localhost:5432`) và tài khoản phải có quyền truy cập database. Việc nạp `.env` không tự tạo database.

Tạo database trước khi chạy service. Flyway migration chỉ tạo schema/bảng bên trong database đã kết nối; không đặt `CREATE DATABASE` trong file migration. Dùng script bootstrap dưới đây một lần khi khởi tạo máy local; script chỉ tạo database đang thiếu, không xóa hoặc ghi đè database đã có:

```powershell
cd E:\dynamicmart
psql -h localhost -p 5432 -U postgres -d postgres -f .\infra\postgres\00_create_local_databases.sql
```

Sau đó chạy từng service. Flyway tạo bảng và ghi lịch sử vào bảng `flyway_schema_history`; trên cùng database, một migration đã chạy sẽ không chạy lại. Với database mới, toàn bộ migration sẽ chạy để tạo schema rỗng.

## Các biến cần cho đăng ký và đăng nhập local

- `services/identity-service/.env`: điền `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` đúng với PostgreSQL của `identity_db`; đặt `SPRING_PROFILES_ACTIVE=local`, `SERVER_PORT=8081`. `JWT_HMAC_SECRET_BASE64` phải giải mã được ít nhất 32 byte và `JWT_ISSUER` phải khớp gateway. `JWT_ACCESS_TOKEN_TTL_SECONDS=900` và `JWT_REFRESH_TOKEN_TTL_DAYS=30` là thời hạn token. `AUTH_REFRESH_COOKIE_SECURE=false` chỉ phù hợp khi dùng HTTP trên localhost; khi dùng HTTPS trong môi trường thật, đặt `true`.
- `api-gateway/.env`: đặt `IDENTITY_SERVICE_URL=http://localhost:8081`, `SERVER_PORT=18080`, `FRONTEND_ORIGIN=http://localhost:3000` trên máy này; JWT secret và issuer phải giống identity-service. Gateway không cần biến DB.
- `dynamicmart-frontend/.env.local`: đặt `NEXT_PUBLIC_API_BASE_URL=http://localhost:18080` trên máy này. Chỉ URL Gateway được đưa ra frontend; không đặt mật khẩu DB hoặc khóa JWT trong biến `NEXT_PUBLIC_*`. Nếu frontend chạy cổng khác 3000, đổi `FRONTEND_ORIGIN` của Gateway theo đúng origin đó. Khởi động lại ứng dụng sau khi sửa `.env`.

Trên máy hiện tại, cổng 8080 đã bị một tiến trình Java khác chiếm. Vì vậy `.env` local của Gateway đang dùng `SERVER_PORT=18080` và `.env.local` frontend dùng `NEXT_PUBLIC_API_BASE_URL=http://localhost:18080`. File mẫu vẫn để 8080 cho máy không bị trùng cổng; nếu đổi cổng Gateway, luôn đổi URL frontend cho khớp.

Các `.env.example` đã có giá trị JWT dùng thử để copy cho local/dev. Vì khóa mẫu được chia sẻ trong repository, không dùng nó khi triển khai thật: tạo khóa ngẫu nhiên riêng, đặt giống nhau ở identity và gateway, giữ ngoài Git. Đổi khóa làm access token cũ không còn hợp lệ. `PASSWORD_RESET_TOKEN_TTL_MINUTES` trong file mẫu hiện chưa được nối vào chức năng quên mật khẩu backend; đặt biến này không làm chức năng đó hoạt động.
