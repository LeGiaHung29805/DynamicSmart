# Order Service standalone bằng Postman

Chế độ này chạy **Order Service, PostgreSQL và RabbitMQ thật**. Chỉ các service ngoài phạm vi của Hiếu được thay bằng adapter mock trong tiến trình Order: Identity/Address, Cart, Catalog/Inventory, Voucher và Payment/Shipping.

## Khởi động

```powershell
cd "D:\web\DynamicSmart\services\order-service"
.\scripts\order-standalone.ps1 up
.\scripts\order-standalone.ps1 smoke
```

Không chạy Identity, Catalog, Cart hoặc Payment. Nếu IntelliJ đang giữ cổng `8084`, hãy dừng cấu hình chạy đó trước khi chạy Docker.

## Chạy collection

1. Import `DynamicMart-Order-Standalone.postman_collection.json`.
2. Import và chọn environment `DynamicMart-Order-Standalone.postman_environment.json`.
3. Chạy toàn bộ collection theo đúng thứ tự bằng Collection Runner.

Collection tự lấy token CUSTOMER, CUSTOMER thứ hai và ADMIN; tự đọc UUID fixture; rồi kiểm tra trọn luồng COD, idempotency, chống IDOR, phân quyền và các ca hợp đồng sai.

## Mock data chính

| Dữ liệu | UUID / giá trị |
|---|---|
| Customer | `00000000-0000-4000-8000-000000000002` |
| Customer thứ hai | `00000000-0000-4000-8000-000000000003` |
| Admin | `00000000-0000-4000-8000-000000000001` |
| Địa chỉ customer | `01000000-0000-4000-8000-000000000001` |
| Cart customer | `70000000-0000-4000-8000-000000000001` |
| Phone variant | `50000000-0000-0000-0000-000000000001` |
| Laptop variant | `50000000-0000-0000-0000-000000000003` |
| Voucher hàng hóa | `71000000-0000-4000-8000-000000000001` (`WELCOME10`) |
| Voucher giao hàng | `71000000-0000-4000-8000-000000000002` (`FREESHIP50`) |

Có thể xem dữ liệu đang dùng tại `GET http://127.0.0.1:8084/api/v1/standalone/data`. Endpoint và API cấp token chỉ tồn tại khi profile `standalone` được bật.

## Dừng

```powershell
.\scripts\order-standalone.ps1 down
```
