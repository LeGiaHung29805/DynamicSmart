# Kịch bản Postman — Order & Checkout

Hai file để import:

- `DynamicMart-Order-Checkout.postman_environment.json`
- `DynamicMart-Order-Checkout.postman_collection.json`

Collection kiểm tra **Order Service của Hiếu** qua cổng trực tiếp, nên không phụ thuộc API Gateway:

| Biến | Mặc định | Mục đích |
|---|---:|---|
| `identityBaseUrl` | `http://localhost:8081` | đăng nhập và lấy địa chỉ sở hữu của khách |
| `orderBaseUrl` | `http://localhost:8084` | toàn bộ API Order/Checkout |
| `variantId` | variant demo local | tạo phiên `BUY_NOW` |

## Điều kiện trước khi chạy

1. Identity, Catalog, Cart, Payment và Order Service đều đang chạy. Order gọi chúng nội bộ khi tạo session/preview/tạo order.
2. Customer demo và admin demo tồn tại; collection có sẵn giá trị local mặc định.
3. Customer demo có ít nhất một địa chỉ hợp lệ trong Identity Service.
4. GHN phải có token dev hợp lệ, Payment Service đã restart và danh mục địa chỉ đã được đồng bộ. Nếu chưa đạt, request **Preview price and shipping quote** sẽ trả lỗi GHN; đó là lỗi phụ thuộc môi trường chứ không phải collection.

## Thứ tự chạy và ý nghĩa

| Folder | Xác nhận |
|---|---|
| `00` | health và endpoint checkout buộc có JWT |
| `01` | đăng nhập CUSTOMER/ADMIN, chọn địa chỉ của đúng customer |
| `02` | tạo `BUY_NOW` session; chặn cặp `PREPAID + COD`; lưu COD hợp lệ |
| `03` | reload giá/voucher/địa chỉ ở server và lấy báo giá GHN |
| `04` | tạo order COD, kiểm tra `Idempotency-Key` không tạo trùng |
| `05` | customer chỉ đọc đơn của mình, không đọc được API admin; admin đọc danh sách được |
| `06` | `CONFIRMED → PACKING → SHIPPING → HANDOVER_PENDING → COMPLETED` và thu COD |
| `07` | chặn tái sử dụng idempotency key giữa hai thao tác; chặn UUID sai |

Chạy folder theo thứ tự `00 → 07`. Folder `03 → 07` **ghi dữ liệu thật vào database local** và hoàn tất một đơn COD. Muốn chạy lại toàn flow, chạy lại từ folder `02` để có checkout session và idempotency key mới.

## Kết quả kỳ vọng quan trọng

- Tạo session trả `201`; không có token trả `401`.
- Cặp `PREPAID + COD` trả `422 INVALID_PAYMENT_SELECTION`.
- Preview thành công trả quote còn hạn và `finalTotalVnd` do server tính.
- Lần tạo order đầu trả `201`, replay cùng `Idempotency-Key` trả `200` với cùng `orderId` và `replay=true`.
- CUSTOMER gọi `/api/v1/orders/admin` trả `403`.
- Lifecycle COD hoàn thành ở `COMPLETED`.

Không dán `GHN_TOKEN`, `INTERNAL_API_KEY`, key thanh toán hoặc JWT thật vào collection để commit. Nhập chúng trong Postman environment local nếu sau này mở rộng collection sang Payment/GHN trực tiếp.
