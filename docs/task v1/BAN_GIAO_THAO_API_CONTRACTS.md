# Bàn giao contract — Thảo: Identity, Cart, Direct Sale và Voucher

Mọi endpoint bên dưới đi qua API Gateway trừ contract `/internal/**`, vốn được gọi trực tiếp giữa các service và bắt buộc header `X-Internal-Api-Key`.

## Identity

- `GET/PUT /api/v1/profile`: đọc/cập nhật hồ sơ của subject trong JWT.
- `GET/POST /api/v1/addresses`: xem/tạo địa chỉ của subject.
- `PUT /api/v1/addresses/{id}`: sửa địa chỉ chính chủ.
- `POST /api/v1/addresses/{id}/default`: đặt mặc định.
- `DELETE /api/v1/addresses/{id}`: chuyển địa chỉ sang `INACTIVE`.
- `GET /api/v1/admin/users`: tìm kiếm/lọc user.
- `PATCH /api/v1/admin/users/{id}`: đổi role/status; bắt buộc `Idempotency-Key` và `reason`.

Identity lấy Province/Ward canonical từ Payment Service. Tên địa giới do browser gửi không được tin khi lưu.

## Cart

- `GET /api/v1/cart`
- `POST /api/v1/cart/items`
- `PATCH /api/v1/cart/items/{itemId}`: gửi `quantity`, `selected`, `version`; version cũ trả `409`.
- `DELETE /api/v1/cart/items/{itemId}`

Cart không trả giá authoritative. Catalog/Checkout phải tải lại trạng thái Variant, giá và tồn.

## Direct Sale

- `GET /api/v1/cart/promotions/prices/{variantId}?listPriceVnd=...`: trả `listPriceVnd`, `discountVnd`, `salePriceVnd`, phần trăm, campaign và hạn sale.
- `GET/POST /api/v1/cart/admin/direct-sales`
- `GET/PUT /api/v1/cart/admin/direct-sales/{id}`
- `PATCH /api/v1/cart/admin/direct-sales/{id}/status`
- `GET /api/v1/cart/admin/direct-sales/{id}/audits`

Mutation admin bắt buộc `Idempotency-Key` và `reason`. Khi kích hoạt, backend chặn hai campaign trùng Variant và thời gian.

## Voucher

- `GET /api/v1/cart/vouchers/wallet`: voucher mặc định và voucher được cấp riêng của customer.
- `POST /api/v1/cart/vouchers/preview`: xem eligibility/discount, không giữ quota.
- `GET/POST /api/v1/cart/admin/vouchers`
- `PUT /api/v1/cart/admin/vouchers/{id}`
- `PATCH /api/v1/cart/admin/vouchers/{id}/status`
- `POST /api/v1/cart/admin/vouchers/{id}/assignments`
- `DELETE /api/v1/cart/admin/voucher-assignments/{assignmentId}?reason=...`
- `GET /api/v1/cart/admin/vouchers/{id}/audits`
- `GET /api/v1/cart/admin/voucher-reservations`: lịch sử giữ/dùng/trả/hết hạn.

Backend hỗ trợ `DEFAULT_FOR_ELIGIBLE`, `ASSIGNED_ONLY`, `CODE_ONLY`; giảm toàn đơn, phí giao hàng, Product, Category và danh sách Product.

## Contract nội bộ cho Order Service

- `POST /api/v1/cart/internal/voucher-reservations`: validate lại và giữ quota. Trùng `(checkoutSessionId, voucherId)` trả cùng reservation.
- `POST /api/v1/cart/internal/voucher-reservations/{id}/consume`: body có `orderId`; gọi lặp cùng Order an toàn.
- `POST /api/v1/cart/internal/voucher-reservations/{id}/release`: chỉ release trạng thái `RESERVED`; gọi lặp an toàn; voucher đã `CONSUMED` không được trả.
- `POST /api/v1/cart/internal/order-confirmed`: dọn giỏ theo `eventId`, `customerId`, `source` và snapshot `{id, quantity, version}`.

Cleanup chỉ chạy khi `source = CART`, item vẫn được chọn và quantity/version còn khớp. `BUY_NOW` hoặc item đã đổi không bị xóa. `eventId` đã xử lý được bỏ qua.
Reservation quá `reservedUntil` được worker chuyển sang `EXPIRED`, nhờ đó quota được giải phóng kể cả khi caller không gửi release.

## Vô hiệu hóa phiên cũ

JWT mang `authVersion`. Sau khi admin đổi role/status, Identity tăng version và thu hồi refresh token. API Gateway gọi contract kiểm tra phiên trực tiếp tới Identity; token có version cũ hoặc user không còn `ACTIVE` nhận `401 SESSION_INVALID`.

Các service phải dùng cùng `JWT_HMAC_SECRET_BASE64` và cùng `INTERNAL_API_KEY` (tối thiểu 24 ký tự trong môi trường triển khai).
