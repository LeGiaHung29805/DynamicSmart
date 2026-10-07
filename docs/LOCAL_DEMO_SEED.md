# Seeder dữ liệu local cho toàn bộ DynamicMart

## Nguyên tắc chọn dữ liệu

Schema trong migration của code mới nhất là nguồn sự thật. Không lấy các dòng đang có trong PostgreSQL cũ để suy ngược schema, vì database local có thể chứa dữ liệu thử hoặc lịch sử Flyway của phiên bản cũ.

Seeder được chia theo service sở hữu dữ liệu; không tạo một file SQL khổng lồ ghi chéo vào nhiều database. Các UUID dùng chung là logical reference, không tạo foreign key xuyên database.

Seeder chỉ được nạp khi chạy profile `local` hoặc khi gọi script Flyway local. Profile mặc định chỉ chạy `db/migration`, vì vậy dữ liệu demo không đi vào môi trường thật.

## Bộ dữ liệu hiện có

| Database | Dữ liệu demo chính |
|---|---|
| `identity_db` | 1 Admin, 2 Customer, 2 địa chỉ |
| `catalog_db` | 3 danh mục, 3 thuộc tính, 7 lựa chọn, 2 sản phẩm, 3 Variant, ảnh, tồn kho và rating tổng hợp |
| `cart_db` | 1 giỏ hàng, 2 dòng giỏ, 1 direct-sale, voucher `DYNAMIC10`, `WELCOME10`, `FREESHIP50` và voucher đã cấp cho Customer |
| `order_db` | 2 checkout session, 1 đơn VNPay hoàn thành, 1 đơn COD chờ bàn giao, snapshot giá/voucher/giao hàng và lịch sử trạng thái |
| `payment_db` | địa giới GHN tối thiểu, 1 payment VNPay đã trả, 1 payment COD đang chờ và 1 payment attempt |
| `engagement_db` | wishlist, review, ảnh review, hỏi đáp, thông báo, chat và metric báo cáo |

`ai-assistant-service` hiện không sở hữu PostgreSQL nên không có Flyway Seeder.

Các file seed nằm tại:

```text
services/<service>/src/main/resources/db/seed/local/
```

## ID liên dịch vụ quan trọng

| Đối tượng | ID |
|---|---|
| Admin | `00000000-0000-4000-8000-000000000001` |
| Customer demo | `00000000-0000-4000-8000-000000000002` |
| Địa chỉ Customer | `01000000-0000-4000-8000-000000000001` |
| Dynamic Phone Pro | `40000000-0000-0000-0000-000000000001` |
| Phone Variant 128 GB | `50000000-0000-0000-0000-000000000001` |
| Giỏ hàng Customer | `70000000-0000-4000-8000-000000000001` |
| Voucher `WELCOME10` | `71000000-0000-4000-8000-000000000001` |
| Voucher `FREESHIP50` | `70000000-0000-0000-0000-000000000002` |
| Order hoàn thành | `80000000-0000-4000-8000-000000000001` |
| Order COD | `80000000-0000-4000-8000-000000000002` |
| Payment VNPay | `90000000-0000-4000-8000-000000000001` |
| Review của Order hoàn thành | `a1000000-0000-4000-8000-000000000001` |

Tài khoản đăng nhập local:

- Admin: `admin@dynamicmart.local` / `Password@123`
- Customer: `customer@dynamicmart.local` / `Password@123`
- Customer 2: `customer2@dynamicmart.local` / `Password@123`

## Nạp Seeder cho cả dự án

Yêu cầu: PostgreSQL đang chạy, JDK 17 và Maven Wrapper của dự án dùng được.

### 1. Tạo sáu database còn thiếu

Từ thư mục gốc repository:

```powershell
psql -h localhost -p 5432 -U postgres -d postgres -f .\infra\postgres\00_create_local_databases.sql
```

Script trên không xóa database và không ghi đè dữ liệu.

### 2. Chuẩn bị credential

Có thể dùng `.env` riêng của từng service. Cách ngắn nhất là cấu hình đúng `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` trong một file local, ví dụ `services/catalog-service/.env`, rồi dùng file đó làm credential chung. Tất cả database local thường dùng cùng một PostgreSQL account.

Không commit file `.env`.

### 3. Chạy migration và seed

```powershell
.\infra\postgres\seed-all.ps1 -SharedEnvFile .\services\catalog-service\.env
```

Script gọi Flyway theo thứ tự Identity → Catalog → Cart → Order → Payment → Engagement. Mỗi service chạy cả `db/migration` và `db/seed/local`; không cần khởi động HTTP service hoặc RabbitMQ.

`-SharedEnvFile` chỉ cấp credential cho lần chạy Seeder, không tự sửa `.env` của các service. Trước khi chạy từng service, hãy bảo đảm `DB_URL` của service đó dùng đúng host/port PostgreSQL thực tế và đúng tên database tương ứng.

Nếu đã có `.env` đúng trong cả sáu service:

```powershell
.\infra\postgres\seed-all.ps1
```

Có thể chỉ chạy một service:

```powershell
.\infra\postgres\seed-all.ps1 -Service catalog-service -SharedEnvFile .\services\catalog-service\.env
```

Kiểm tra Flyway mà không sửa dữ liệu:

```powershell
.\infra\postgres\seed-all.ps1 -Command validate -SharedEnvFile .\services\catalog-service\.env
```

Mặc định script từ chối host khác `localhost`/`127.0.0.1` để tránh seed nhầm môi trường thật.

### 4. Kiểm tra dữ liệu đầu ra

```powershell
psql -h localhost -p 5432 -U postgres -d postgres -f .\infra\postgres\01_verify_local_seed.sql
```

Kết quả cuối phải có:

```text
SEED VERIFY PASS: 6 database local có dữ liệu demo cốt lõi.
```

## Khi database cũ không còn khớp Flyway

Nhánh mới đã đổi/thêm migration ở Catalog, Cart, Order, Payment và Engagement. Nếu Flyway báo checksum mismatch, duplicate version hoặc migration đã bị đổi tên, không chạy `repair` một cách mù quáng.

- Nếu dữ liệu local không cần giữ: backup nếu cần, xóa đúng sáu database local, tạo lại bằng `00_create_local_databases.sql`, rồi chạy `seed-all.ps1`.
- Nếu dữ liệu cần giữ: dump database trước và viết migration chuyển đổi riêng; không dùng dữ liệu cũ thay cho Seeder chuẩn.

Không xóa database dùng chung của nhóm hoặc database production.
