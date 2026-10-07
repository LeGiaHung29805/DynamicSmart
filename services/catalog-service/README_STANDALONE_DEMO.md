# Demo độc lập Catalog & Inventory

Chế độ `standalone-demo` giúp trình diễn toàn bộ phần việc Catalog/Inventory khi Identity, Cart, Order, Promotion, Engagement và RabbitMQ của các thành viên khác chưa chạy. PostgreSQL `catalog_db` vẫn là dữ liệu thật; các phần thay thế chỉ hoạt động trong profile demo và không làm thay đổi kiến trúc tích hợp chính thức.

## Phạm vi có thể tự demo

- Danh mục, thuộc tính, sản phẩm, ảnh và Variant phía Admin.
- Duyệt, tìm kiếm, lọc, sắp xếp mới nhất/giá/bán chạy và xem chi tiết phía Customer.
- Giá khuyến mại demo, rating tổng hợp và thứ tự bán chạy mẫu.
- Điều chỉnh, xem lịch sử và kiểm tra tồn kho.
- Giữ/chốt/hoàn tồn qua internal API với idempotency và khóa cạnh tranh.
- Đăng nhập Customer/Admin, thêm giỏ và bàn giao phiên Mua ngay mà không cần service khác.

## 1. Chuẩn bị

Yêu cầu: JDK 17, PostgreSQL đang chạy và database `catalog_db` đã được tạo.

Trong `services/catalog-service`, sao chép `.env.example` thành `.env`, sau đó điền `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_HMAC_SECRET_BASE64` và `INTERNAL_API_KEY`. File `.env` không được commit.

Kiểm tra môi trường từ thư mục gốc dự án:

```powershell
.\services\catalog-service\scripts\catalog-standalone.ps1 doctor
```

## 2. Chạy backend độc lập

Terminal thứ nhất:

```powershell
.\services\catalog-service\scripts\catalog-standalone.ps1 run
```

Flyway tự tạo/cập nhật schema và nạp dữ liệu minh họa. Service chạy ở `http://localhost:8082`; không cần khởi động RabbitMQ.

Terminal thứ hai, chạy kiểm thử xuyên suốt:

```powershell
.\services\catalog-service\scripts\catalog-standalone.ps1 smoke
```

Kết quả hợp lệ kết thúc bằng `SMOKE PASS`.

## 3. Chạy frontend

```powershell
Copy-Item .\dynamicmart-frontend\.env.standalone.example .\dynamicmart-frontend\.env.local
Set-Location .\dynamicmart-frontend
npm install
npm run dev
```

Mở `http://localhost:3000` và dùng một trong hai tài khoản:

- Customer: `demo@dynamicmart.local` / `Demo@123`
- Admin: `admin@dynamicmart.local` / `Demo@123`

Email bắt đầu bằng `admin@` nhận role `ADMIN`; email khác nhận role `CUSTOMER`. Có thể dùng màn hình đăng ký với mật khẩu tối thiểu 8 ký tự trong phiên chạy hiện tại.

## 4. Trở lại chế độ tích hợp

Đặt `NEXT_PUBLIC_CATALOG_STANDALONE_DEMO=false`, trỏ frontend về API Gateway và chạy Catalog chỉ với profile `local`. Khi đó:

- Identity cấp token thật.
- Cart Promotion trả giá khuyến mại qua HTTP; nếu phản hồi chậm/lỗi, Catalog hết thời gian chờ và an toàn quay về giá niêm yết.
- Order/Cart gọi internal Inventory API.
- Reporting của Engagement cung cấp thứ tự bestseller; Engagement cập nhật rating tổng hợp qua `/api/v1/catalog/internal/products/{productId}/rating` và tiếp tục sở hữu nội dung review.
- Outbox tiếp tục phát event qua RabbitMQ.

Các API `/api/v1/catalog/demo/**` hoàn toàn không tồn tại nếu profile `standalone-demo` không được bật.
