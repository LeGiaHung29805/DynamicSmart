# Payment Service API — Người 4

Các API đều đi qua Gateway. Browser chỉ gọi API công khai lấy địa giới và luồng Checkout của Order Service; không được gọi trực tiếp API quote hay gửi `OrderPaymentContext`.

| API | Caller | Mục đích |
|---|---|---|
| `GET /api/v1/locations/provinces` | Address UI | Danh mục Tỉnh/Thành đang active. |
| `GET /api/v1/locations/wards?provinceId=` | Address UI | Danh mục Phường/Xã của tỉnh. |
| `GET /api/v1/locations/validate?provinceId=&wardId=` | Identity Service | Kiểm tra Ward thuộc Province trước khi lưu Address. |
| `POST /api/v1/locations/sync` | Operations | Đồng bộ catalog GHN; phải được bảo vệ ở Gateway. |
| `POST /api/v1/shipping/quotes` | Order Service | Quote từ address/item snapshot tin cậy, trả fee/service/ETA/TTL và `requestFingerprint` canonical để Order consume đúng quote. |
| `POST /api/v1/shipping/quotes/{id}/validate` | Order Service | Xác thực và dùng đúng một lần quote trước khi snapshot vào Order. |
| `POST /api/v1/payments/order-context` | Order Service | Tạo Payment từ OrderPaymentContext; chỉ gọi service-to-service. |
| `GET /api/v1/payments/orders/{orderId}` | Order Service | Lấy trạng thái Payment và attempt online mới nhất, dùng để hiển thị thanh toán trả sau sau event `PaymentDue`. |
| `POST /api/v1/payments/{id}/attempts` | Order Service | Tạo URL/QR cho VNPay, ZaloPay, PayOS hoặc QR ngân hàng khi Payment còn `PENDING`. |
| `POST /api/v1/payments/{id}/vnpay-attempt` | Order Service | Alias tương thích ngược của API tạo attempt. |
| `GET /api/v1/payments/my-orders/{orderId}` | Customer order UI | Đọc Payment thuộc chính customer, kèm URL attempt còn hiệu lực nếu có. Gateway tự gắn danh tính xác thực; frontend không gửi `customerId`. |
| `POST /api/v1/payments/my-orders/{orderId}/attempts` | Customer order UI | Tiếp tục hoặc tạo lại URL/QR online bằng dữ liệu Payment phía server. Trả sau chỉ dùng được sau khi `PaymentDue` đã tạo attempt đầu tiên. |
| `GET/POST /api/v1/payments/vnpay/ipn` | VNPay | Xác minh IPN, audit và ghi Payment đúng một lần. `GET` là phương thức callback chuẩn; `POST` được giữ để dễ kiểm thử/tương thích. |
| `POST /api/v1/payments/zalopay/callback` | ZaloPay | Kiểm tra HMAC bằng Key2, amount/reference và xử lý callback đúng một lần. |
| `POST /api/v1/payments/payos/webhook` | PayOS | Kiểm tra checksum webhook trước khi ghi nhận thanh toán. |
| `POST /api/v1/payments/sepay/webhook` | SePay | Kiểm tra HMAC trên raw body và timestamp, sau đó đối soát mã chuyển khoản/số tiền. |
| `GET /api/v1/payments/return-status?reference=` | Customer return page | Đọc trạng thái authoritative của mọi cổng online theo reference và customer JWT. |
| `GET /api/v1/payments/vnpay/return-status?vnp_TxnRef=` | Customer return page | Đọc trạng thái authoritative theo reference và `sub` JWT; không tin query trạng thái từ trình duyệt. |
| `GET /api/v1/payments?page=&size=` | Admin | Danh sách Payment phân trang, không trả URL VNPay hoặc secret. |
| `GET /api/v1/payments/{id}/attempts` | Admin | Lịch sử lần thử đã lọc URL nhạy cảm. |
| `GET /api/v1/payments/{id}/callback-audits` | Admin | Audit callback/IPN đã redaction. |
| `POST /api/v1/payments/orders/{orderId}/cod-collected` | Internal Order Service | Được gọi từ `POST /api/v1/orders/{orderId}/received` sau khi chính khách hàng bấm **Đã nhận hàng**; không nhập số tiền hoặc biên nhận. |

Consumer `paymentDue` nhận event envelope chuẩn (`eventId`, `eventType`, `eventVersion`, `producer`, `aggregateId`, `occurredAt`, `correlationId`, `payload.orderId`) từ destination `payment.due`. Trong giai đoạn chuyển đổi contract, consumer vẫn đọc được message cũ có `orderId` dạng phẳng. Event bị gửi lại chỉ được xử lý một lần nhờ `processed_events`; Order `POSTPAID` dùng VNPay, ZaloPay, PayOS hoặc QR ngân hàng và có Payment `PENDING` mới tạo attempt online.

Các API service-to-service yêu cầu header `X-Internal-Api-Key`, với giá trị `INTERNAL_API_KEY`; Gateway chỉ chuyển tiếp header này, còn browser không được có giá trị đó. `VNPAY_HASH_SECRET`, `GHN_TOKEN`, `GHN_SHOP_ID` và kho gửi chỉ có trong môi trường triển khai. Nếu GHN/VNPay chưa cấu hình, service trả `GHN_NOT_CONFIGURED` hoặc `VNPAY_NOT_CONFIGURED`; không trả phí hoặc URL giả.

Biến URL thanh toán ưu tiên `VNPAY_PAYMENT_URL`; cấu hình Laravel cũ dùng `VNPAY_URL` vẫn được chấp nhận để chuyển đổi thuận tiện. `VNPAY_IPN_URL` được khai báo tại cổng VNPay và phải trỏ tới Gateway/public tunnel ở đường dẫn `/api/v1/payments/vnpay/ipn`; Payment Service không cần đọc biến này để tạo URL thanh toán.

Frontend tại `/checkout` và `/checkout/payment` dùng cùng wizard: địa chỉ → báo giá GHN/mã giảm giá → phương thức thanh toán → xác nhận. Wizard đã nối Checkout Session/Preview và Create Order của Order Service; mọi giá, tồn kho, voucher, quote và Payment context đều được các service xác minh lại ở máy chủ. Trang `/customer/account/orders/{id}` hiển thị read model thật, trạng thái Payment, URL còn hiệu lực và thao tác tạo lại attempt được bảo vệ theo ownership.

Payment Service nhận `PaymentDue` và phát các event thanh toán qua Outbox. Order Service phát `PaymentDue` cho mọi phương thức online trả sau khi đơn đi vào `HANDOVER_PENDING`, đồng thời xử lý `PaymentSucceeded`/`PaymentFailed`/`PaymentExpired` theo state machine đã chốt.

GHN tổ chức Ward dưới District. Payment Service vì vậy cache `ghn_location_districts` và `ghn_ward_code` làm metadata nội bộ để gọi API fee, nhưng contract Address vẫn chỉ công khai `provinceId` và `wardId`; trình duyệt không chọn hoặc lưu District.
