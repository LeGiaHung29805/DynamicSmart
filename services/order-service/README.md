# Order Service

Môi trường local chạy Order Service với PostgreSQL, Flyway và RabbitMQ thật. Order Service luôn dùng HTTP adapter
thật để tích hợp Identity, Cart, Catalog và Payment; repository không cung cấp adapter giả ở runtime.

## Chạy local

Yêu cầu duy nhất ngoài repository là Docker Desktop đang hoạt động. Mở PowerShell tại thư mục này:

```powershell
.\scripts\order-local.ps1 up
.\scripts\order-local.ps1 smoke
```

Sau khi `up` hoàn tất:

- API: `http://127.0.0.1:8084`;
- health: `http://127.0.0.1:8084/actuator/health`;
- RabbitMQ Management: `http://127.0.0.1:15674` với `order_user/order_password`.

Smoke script kiểm tra health, Flyway V8 và RabbitMQ thật. Nó không tự tạo JWT hay dữ liệu nghiệp vụ. Để kiểm tra
API có xác thực, đặt `ORDER_LOCAL_ACCESS_TOKEN` bằng access token do Identity Service thật cấp trước khi chạy
`smoke`. Muốn chạy Checkout/Create Order hoàn chỉnh, các service thật phải hoạt động ở cổng `8081`, `8082`,
`8083` và `8085` với cùng JWT/internal key.

```powershell
.\scripts\order-local.ps1 status
.\scripts\order-local.ps1 logs
.\scripts\order-local.ps1 down
```

Credentials trong Compose chỉ dành cho local; không dùng ở staging/production. Volume PostgreSQL và RabbitMQ
được giữ sau `down`; chỉ `down -v` mới xóa dữ liệu.
Quy trình tích hợp đầy đủ nằm tại `../../docs/ORDER_SERVICE_RUNBOOK.md`.
