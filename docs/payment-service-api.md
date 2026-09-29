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
| `GET /api/v1/payments/orders/{orderId}` | Order Service | Lấy trạng thái Payment và URL VNPay attempt mới nhất, dùng để hiển thị thanh toán trả sau sau event `PaymentDue`. |
| `POST /api/v1/payments/{id}/attempts` | Order Service | Tạo URL/QR cho VNPay, ZaloPay, PayOS hoặc QR ngân hàng khi Payment còn `PENDING`. |
| `POST /api/v1/payments/{id}/vnpay-attempt` | Order Service | Alias tương thích ngược của API tạo attempt. |
| `GET/POST /api/v1/payments/vnpay/ipn` | VNPay | Xác minh IPN, audit và ghi Payment đúng một lần. `GET` là phương thức callback chuẩn; `POST` được giữ để dễ kiểm thử/tương thích. |
| `POST /api/v1/payments/zalopay/callback` | ZaloPay | Kiểm tra HMAC bằng Key2, amount/reference và xử lý callback đúng một lần. |
| `POST /api/v1/payments/payos/webhook` | PayOS | Kiểm tra checksum webhook trước khi ghi nhận thanh toán. |
| `POST /api/v1/payments/sepay/webhook` | SePay | Kiểm tra HMAC trên raw body và timestamp, sau đó đối soát mã chuyển khoản/số tiền. |
| `GET /api/v1/payments/return-status?reference=` | Customer return page | Đọc trạng thái authoritative của mọi cổng online theo reference và customer JWT. |
| `GET /api/v1/payments/vnpay/return-status?vnp_TxnRef=` | Customer return page | Đọc trạng thái authoritative theo reference và `sub` JWT; không tin query trạng thái từ trình duyệt. |
| `GET /api/v1/payments?page=&size=` | Admin | Danh sách Payment phân trang, không trả URL VNPay hoặc secret. |
| `GET /api/v1/payments/{id}/attempts` | Admin | Lịch sử lần thử đã lọc URL nhạy cảm. |
| `GET /api/v1/payments/{id}/callback-audits` | Admin | Audit callback/IPN đã redaction. |
| `POST /api/v1/payments/{id}/cod-confirmations` | Admin | Xác nhận COD với số tiền và biên nhận; Gateway lấy admin ID từ JWT và ghi đè header danh tính, không tin ID trong body. |

Consumer `paymentDue` nhận event `PaymentDue(eventId, orderId, correlationId)` từ destination `payment.due`. Event bị gửi lại chỉ được xử lý một lần nhờ `processed_events`; chỉ order `POSTPAID + VNPAY` đã có Payment `PENDING` mới tạo URL/QR.

Các API service-to-service yêu cầu header `X-Internal-Api-Key`, với giá trị `INTERNAL_API_KEY`; Gateway chỉ chuyển tiếp header này, còn browser không được có giá trị đó. `VNPAY_HASH_SECRET`, `GHN_TOKEN`, `GHN_SHOP_ID` và kho gửi chỉ có trong môi trường triển khai. Nếu GHN/VNPay chưa cấu hình, service trả `GHN_NOT_CONFIGURED` hoặc `VNPAY_NOT_CONFIGURED`; không trả phí hoặc URL giả.

Biến URL thanh toán ưu tiên `VNPAY_PAYMENT_URL`; cấu hình Laravel cũ dùng `VNPAY_URL` vẫn được chấp nhận để chuyển đổi thuận tiện. `VNPAY_IPN_URL` được khai báo tại cổng VNPay và phải trỏ tới Gateway/public tunnel ở đường dẫn `/api/v1/payments/vnpay/ipn`; Payment Service không cần đọc biến này để tạo URL thanh toán.

Frontend wizard ở `/checkout/payment` chia thành: địa chỉ → báo giá GHN → phương thức thanh toán → xác nhận. Nó đang dùng tóm tắt đơn fixture, chờ Checkout/Order Service của Hiếu cung cấp session/order context có thẩm quyền.

GHN tổ chức Ward dưới District. Payment Service vì vậy cache `ghn_location_districts` và `ghn_ward_code` làm metadata nội bộ để gọi API fee, nhưng contract Address vẫn chỉ công khai `provinceId` và `wardId`; trình duyệt không chọn hoặc lưu District.
