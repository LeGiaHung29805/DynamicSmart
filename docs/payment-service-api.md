# Payment Service API — Người 4

Các API đều đi qua Gateway. Browser chỉ gọi API công khai lấy địa giới và luồng Checkout của Order Service; không được gọi trực tiếp API quote hay gửi `OrderPaymentContext`.

| API | Caller | Mục đích |
|---|---|---|
| `GET /api/v1/locations/provinces` | Address UI | Danh mục Tỉnh/Thành đang active. |
| `GET /api/v1/locations/wards?provinceId=` | Address UI | Danh mục Phường/Xã của tỉnh. |
| `GET /api/v1/locations/validate?provinceId=&wardId=` | Identity Service | Kiểm tra Ward thuộc Province trước khi lưu Address. |
| `POST /api/v1/locations/sync` | Operations | Đồng bộ catalog GHN; phải được bảo vệ ở Gateway. |
| `POST /api/v1/shipping/quotes` | Order Service | Quote từ address/item snapshot tin cậy, trả fee/service/ETA/TTL. |
| `POST /api/v1/shipping/quotes/{id}/validate` | Order Service | Xác thực và dùng đúng một lần quote trước khi snapshot vào Order. |
| `POST /api/v1/payments/order-context` | Order Service | Tạo Payment từ OrderPaymentContext; chỉ gọi service-to-service. |
| `POST /api/v1/payments/{id}/vnpay-attempt` | Order Service | Tạo URL/QR VNPay cho khoản `PENDING`. |
| `POST /api/v1/payments/vnpay/ipn` | VNPay | Xác minh IPN, audit và ghi Payment đúng một lần. |
| `POST /api/v1/payments/{id}/cod-confirmations` | Admin | Xác nhận COD với số tiền, biên nhận, admin ID. |

Consumer `paymentDue` nhận event `PaymentDue(eventId, orderId, correlationId)` từ destination `payment.due`. Event bị gửi lại chỉ được xử lý một lần nhờ `processed_events`; chỉ order `POSTPAID + VNPAY` đã có Payment `PENDING` mới tạo URL/QR.

Các API service-to-service yêu cầu header `X-Internal-Api-Key`, với giá trị `INTERNAL_API_KEY`; Gateway chỉ chuyển tiếp header này, còn browser không được có giá trị đó. `VNPAY_HASH_SECRET`, `GHN_TOKEN`, `GHN_SHOP_ID` và kho gửi chỉ có trong môi trường triển khai. Nếu GHN/VNPay chưa cấu hình, service trả `GHN_NOT_CONFIGURED` hoặc `VNPAY_NOT_CONFIGURED`; không trả phí hoặc URL giả.

Frontend wizard ở `/checkout/payment` chia thành: địa chỉ → báo giá GHN → phương thức thanh toán → xác nhận. Nó đang dùng tóm tắt đơn fixture, chờ Checkout/Order Service của Hiếu cung cấp session/order context có thẩm quyền.
