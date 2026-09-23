# Kế hoạch triển khai Order Service của Hiếu

## 1. Mục đích tài liệu

Tài liệu này mô tả đầy đủ phạm vi, hiện trạng, kế hoạch triển khai, điểm tích hợp và cách theo dõi tiến độ cho phần việc của thành viên Hiếu trong dự án DynamicMart.

Người mới tham gia dự án có thể dùng tài liệu này để trả lời các câu hỏi:

- Hiếu sở hữu phần nào của hệ thống?
- Order Service hiện đã có gì và còn thiếu gì?
- Việc nào có thể làm ngay mà không cần chờ thành viên khác?
- Order Service cần nhận và cung cấp dữ liệu gì?
- Trình tự triển khai và tiêu chí hoàn thành của từng giai đoạn là gì?
- Làm thế nào kiểm tra Checkout, Order, Saga và xử lý sự kiện đã đúng?

Tài liệu được lập ngày **2026-09-23**, dựa trên trạng thái nhánh `main` tại commit `7506e43` và được cập nhật theo nhánh triển khai `VanHieu/order-checkout-foundation`.

## 2. Tài liệu nguồn và thứ tự ưu tiên

Khi có nội dung khác nhau giữa các tài liệu, ưu tiên theo thứ tự sau:

1. `docs/00_OVERVIEW.md`.
2. `docs/task v1/NHIEM_VU_HIEU_CHECKOUT_ORDER_SAGA.md`.
3. `docs/06_PHAN_LAM_CHUNG.md`.
4. `docs/09_DATABASE_DESIGN.md`.
5. `docs/10_DANH_SACH_CHUC_NANG.md`.
6. `docs/11_BACKEND_CODING_RULES.md`.
7. `docs/12_FRONTEND_CODING_RULES.md`.
8. `docs/13_AUTHENTICATION_FOUNDATION.md`.

`README.md` và `docs/DYNAMICMART_PHAM_VI_KIEN_TRUC_VA_PHAN_CONG.md` có chứa phương án kiến trúc cũ. Không dùng nội dung trong hai tệp này để ghi đè phạm vi hiện hành khi có mâu thuẫn.

## 3. Phạm vi sở hữu của Hiếu

Nếu không tính giao diện, toàn bộ mã nghiệp vụ của Hiếu nằm trong:

```text
services/order-service/
```

Hiếu sở hữu:

- `order-service` và `order_db`.
- Checkout Session cho nguồn `CART` và `BUY_NOW`.
- Checkout Preview và phép tính tổng tiền cuối cùng.
- Tạo Order bằng `Idempotency-Key`.
- Snapshot bất biến của hàng hóa, địa chỉ, voucher, phí giao hàng và phương thức thanh toán.
- Điều phối Saga giữ, chốt và trả tồn kho/voucher.
- State machine của Order.
- Lịch sử trạng thái và audit của Order.
- API danh sách, chi tiết và thao tác Order cho Customer/Admin.
- Nhận sự kiện kết quả thanh toán.
- Phát các sự kiện Order cho Cart, Payment và Engagement.
- Migration, entity, repository, service, controller, client tích hợp, messaging, outbox và kiểm thử của Order Service.

Hiếu không sở hữu và không viết business logic trong:

- `identity-service`.
- `catalog-service`.
- `cart-service`.
- `payment-service`.
- `engagement-service`.

Nếu cần dữ liệu từ các phần trên, Order Service gọi REST contract hoặc nhận sự kiện. Thay đổi contract dùng chung phải được thống nhất với chủ sở hữu service liên quan.

## 4. Hiện trạng repository

### 4.1. Trạng thái chung

- Repository đang dùng nhánh `main` và working tree sạch tại thời điểm lập tài liệu.
- Dự án là mono-repo gồm API Gateway, 6 business service và frontend Next.js.
- Gateway đã có routing, kiểm tra JWT và CORS.
- Identity Service đã có register, login, refresh token và logout.
- Catalog, Cart, Order và Engagement hiện chủ yếu mới có migration cùng một số entity; Payment đã có bước triển khai GHN quote và checkout payment flow.
- Thư mục `contracts/` chưa có contract triển khai thực tế.
- Chưa có `docker-compose.yml` hoàn chỉnh.
- RabbitMQ binder, Outbox publisher và consumer nghiệp vụ chưa được triển khai.
- Test của các service hiện chỉ có `contextLoads`.

### 4.2. Hiện trạng Order Service

Đã có:

- Maven project Spring Boot.
- Spring Data JPA, Flyway, Validation, Web MVC và Spring Cloud Stream dependency.
- `application.yaml` kết nối `order_db`.
- Migration `V1__initial_schema.sql`.
- Các bảng Checkout, Order, snapshot, history, outbox, processed event và idempotency.
- Năm entity cơ bản:
  - `CheckoutSession`.
  - `CheckoutSessionItem`.
  - `CustomerOrder`.
  - `OrderItem`.
  - `OrderAddress`.
- Khung package `client`, `config`, `controller`, `dto`, `exception`, `mapper`, `messaging`, `outbox`, `repository` và `service`.
- Cấu hình mặc định cổng `8084`, Actuator health/info và cấu hình kết nối service phụ thuộc.
- JWT Resource Server dùng HMAC/issuer chung, ánh xạ role Customer/Admin và JSON error cho `401`/`403`.
- Enum nghiệp vụ Checkout, Order, Payment, Saga và quote.
- `Clock` bean, configuration properties có validation và global exception handler.
- Migration V2 bổ sung shipping snapshot, optimistic version, trace history và bảng Saga bền vững.
- Entity/repository cho toàn bộ bảng Checkout, Order, snapshot, Saga, Outbox, processed event và idempotency.
- API Checkout Session: tạo, lấy chi tiết, cập nhật lựa chọn và hủy (`/api/v1/checkout/sessions`).
- DTO cho Checkout Session; controller chỉ lấy `customerId` từ JWT và không trả JPA entity.
- `CheckoutSessionService` tạo snapshot từ `CheckoutSelectionGateway`, kiểm tra expiry/ownership và hủy idempotent.
- Fallback an toàn cho Cart/Catalog: trả `503 CHECKOUT_SOURCE_UNAVAILABLE`, tuyệt đối không sinh giá/tồn kho giả.
- `CheckoutPricingService`, `OrderStateMachine` và Payment/GHN HTTP adapter nền.
- API Checkout Preview điều phối Address, Voucher, GHN quote và tính tiền nhưng không tạo reservation.
- Preview gọi service ngoài transaction, sau đó khóa session để chống ghi snapshot stale; quote cũ bị invalidated.
- Migration V3 và snapshot kích thước/trọng lượng phục vụ GHN package rule.
- 50 automated test hiện có: security, JWT role, payment contract, pricing, Checkout Session/Preview, state machine và context smoke test.

Chưa có:

- API Create Order.
- REST adapter thật sang Identity, Catalog và Cart (hiện chỉ có gateway interface/fallback an toàn).
- Controller/API Order Customer và Admin.
- Saga orchestrator và trạng thái Saga bền vững.
- Xử lý `Idempotency-Key` thực tế.
- Outbox publisher.
- RabbitMQ consumer/producer nghiệp vụ.
- Repository constraint/locking test, controller test và integration test.

### 4.3. Khoảng lệch giữa migration V1 và thiết kế hiện hành

Không sửa migration V1 đã commit. Các thay đổi phải được thực hiện bằng migration mới.

Những điểm cần xử lý:

- `checkout_shipping_quotes` thiếu provider, trọng lượng, kích thước kiện, thông tin địa chỉ đích, thời điểm quote và thời điểm consume.
- Trạng thái quote trong V1 chưa đồng nhất với thiết kế `ACTIVE`, `CONSUMED`, `INVALIDATED`, `EXPIRED`.
- `order_shipping_snapshots` thiếu provider, nguồn Checkout Quote, kho gửi, địa chỉ đích, trọng lượng và kích thước kiện.
- `order_status_history` chưa có actor `PAYMENT_EVENT`, `correlation_id` và `source_event_id`.
- Chưa có nơi lưu inventory reservation ID và tiến trình Saga bền vững.
- Entity mới bao phủ một phần các bảng trong migration.
- Trạng thái nghiệp vụ đang dùng `String`, chưa được giới hạn bằng enum/domain rule.

## 5. Nguyên tắc triển khai

1. Order Service chỉ đọc và ghi `order_db`.
2. Không truy vấn database của service khác.
3. Không nhận `customerId`, giá, tồn kho, giảm giá hoặc phí giao hàng như dữ liệu đáng tin từ frontend.
4. `customerId` được lấy từ JWT đã xác thực.
5. Tiền VND dùng số nguyên `BIGINT`/`long`, không dùng `float` hoặc `double`.
6. Snapshot của Order không được thay đổi khi dữ liệu nguồn thay đổi.
7. Không giữ transaction database trong lúc gọi HTTP hoặc message broker.
8. Event nghiệp vụ được ghi vào Outbox trong cùng local transaction với dữ liệu nghiệp vụ.
9. Consumer phải chống event trùng bằng `processed_events`.
10. Reserve, commit, consume và release phải idempotent.
11. Controller không được chứa business logic hoặc gọi repository trực tiếp.
12. Không trả JPA entity trực tiếp qua API.
13. Không cho controller hoặc frontend tự gán trạng thái Order.

## 6. Kiến trúc dự kiến trong Order Service

```text
controller/
  CheckoutController
  CustomerOrderController
  AdminOrderController

dto/request/
  CreateCheckoutSessionRequest
  UpdateCheckoutSelectionRequest
  CreateOrderRequest
  ConfirmReceivedRequest
  AdminOrderCommandRequest

dto/response/
  CheckoutSessionResponse
  CheckoutPreviewResponse
  OrderSummaryResponse
  OrderDetailResponse
  OrderTimelineResponse

entity/
  CheckoutSession
  CheckoutSessionItem
  CheckoutSessionVoucher
  CheckoutShippingQuote
  CustomerOrder
  OrderItem
  OrderAddress
  OrderVoucherSnapshot
  OrderShippingSnapshot
  OrderStatusHistory
  OrderSaga
  OutboxEvent
  ProcessedEvent
  IdempotencyRecord

repository/
  Repository tương ứng cho aggregate và bảng kỹ thuật

service/
  CheckoutService
  CheckoutPricingService
  OrderCreationService
  OrderQueryService
  OrderCommandService
  OrderStateMachine
  OrderSagaOrchestrator
  IdempotencyService

client/
  IdentityClient
  CatalogClient
  CartClient
  PaymentClient

messaging/consumer/
  PaymentEventConsumer

messaging/producer/
  OutboxEventPublisher

outbox/
  OutboxService
  OutboxPublisherJob
```

Tên lớp có thể thay đổi khi triển khai, nhưng trách nhiệm của từng tầng phải được giữ rõ ràng.

## 7. Kế hoạch theo giai đoạn

### M0. Chốt contract và quyết định kỹ thuật

**Trạng thái: Đang thực hiện**

Việc cần làm:

- Chốt REST contract với Identity, Catalog, Cart và Payment.
- Chốt event envelope dùng chung.
- Chốt mã lỗi tích hợp.
- Chốt timeout, retry và cách xác thực API nội bộ.
- Chốt cách Order Service nhận principal từ JWT.
- Chốt cấu trúc lưu Saga bền vững.
- Chốt Inventory Reservation là một reservation cho toàn Order hay nhiều reservation.

Đầu ra:

- Contract request/response có version.
- Event payload có version.
- Không còn trường dữ liệu quan trọng chỉ tồn tại dưới dạng mô tả miệng.

### M1. Nền service, security và cấu hình

**Trạng thái: Hoàn thành**

Việc cần làm:

- Cấu hình `SERVER_PORT` mặc định là `8084`.
- Thêm Resource Server/JWT dependency và security configuration.
- Lấy `sub` và role từ JWT.
- Tạo error envelope và `GlobalExceptionHandler`.
- Tạo các enum nghiệp vụ.
- Tạo `Clock` bean để kiểm thử thời gian/expiry ổn định.
- Tạo configuration properties cho URL các service, timeout, Checkout TTL và Outbox publisher.

Tiêu chí hoàn thành:

- Service khởi động ở cổng `8084`.
- Health endpoint hoạt động.
- Endpoint được bảo vệ đúng Customer/Admin.
- Không nhận `customerId` từ request.

### M2. Đồng bộ schema và persistence

**Trạng thái: Đang thực hiện**

Việc cần làm:

- Tạo `V2__align_order_schema_with_current_contract.sql`.
- Bổ sung dữ liệu quote/shipping snapshot còn thiếu.
- Bổ sung trace cho status history.
- Thiết kế bảng hoặc cấu trúc lưu Saga bền vững.
- Hoàn thiện toàn bộ entity và repository.
- Bổ sung index, unique constraint và lock query cần thiết.
- Không tạo khóa ngoại sang database khác.

Tiêu chí hoàn thành:

- Flyway chạy thành công trên database sạch.
- JPA validation khớp schema.
- Repository test xác nhận các unique/check constraint quan trọng.

### M3. Checkout Session

**Trạng thái: Đang thực hiện**

API đã có:

```text
POST   /api/v1/checkout/sessions
GET    /api/v1/checkout/sessions/{sessionId}
PATCH  /api/v1/checkout/sessions/{sessionId}
DELETE /api/v1/checkout/sessions/{sessionId}
```

`POST` chỉ nhận selection nhỏ nhất: `CART` nhận `cartId`; `BUY_NOW` nhận `variantId` và `quantity`. Endpoint không nhận item, giá, promotion, trọng lượng hoặc `customerId` từ client. `PATCH` chỉ cập nhật `addressId`, `paymentTiming`, `paymentMethod` và không làm mới snapshot item; chỉ nhận cặp `PREPAID + VNPAY`, `POSTPAID + VNPAY` hoặc `POSTPAID + COD`. `NOT_REQUIRED + FREE` chỉ do backend gán sau khi tính tổng tiền.

`POST /api/v1/checkout/sessions/{sessionId}/preview` chưa triển khai; thuộc M5 khi đã có address, voucher và shipping quote contract.

Nghiệp vụ:

- Chỉ Customer đã đăng nhập được dùng Checkout.
- `CART` lấy các CartItem được chọn.
- `BUY_NOW` chứa đúng một Variant và quantity.
- Checkout Session hết hạn sau 15 phút.
- Preview không reserve tồn kho hoặc voucher.
- Customer chỉ đọc/sửa/hủy session của chính mình.
- Chỉ session `ACTIVE` và chưa có Order mới được hủy.
- Hủy session không xóa Cart và không phát `OrderCancelled`.
- Khi Cart/Catalog chưa cung cấp HTTP contract, gateway fallback trả lỗi `503 CHECKOUT_SOURCE_UNAVAILABLE`; không có fallback dữ liệu giả.

Tiêu chí hoàn thành:

- Tạo CART/BUY_NOW đã được kiểm thử với mock gateway; vẫn cần HTTP adapter thật.
- Session hết hạn không thể update/hủy và sẽ chuyển `EXPIRED`; Create Order chưa tồn tại (M6).
- Hủy lặp không xóa snapshot hoặc gọi Cart/Catalog.
- Ownership được enforced trong service theo `sessionId + customerId`; còn cần controller/security test cho truy cập chéo thực tế.

### M4. Client tích hợp và mock adapter

**Trạng thái: Đang thực hiện**

Việc cần làm:

- Tạo interface cho từng client tích hợp.
- Tạo mock adapter/fixture để phát triển độc lập.
- Tạo HTTP adapter thay thế mock khi API thật có sẵn.
- Cấu hình timeout và map lỗi kỹ thuật thành lỗi nghiệp vụ rõ ràng.
- Không đặt fallback tự ý dùng giá, phí hoặc tồn giả trong production.

Order Service cần nhận:

| Service | Dữ liệu hoặc thao tác |
|---|---|
| Identity | AddressSnapshot, kiểm tra ownership/active |
| Catalog | Variant mua được, giá, trọng lượng, reserve/commit/release inventory |
| Cart | Selected CartItem, version, direct sale, validate/reserve/consume/release voucher |
| Payment | GHN quote, tạo Payment từ OrderPaymentContext |

Tiêu chí hoàn thành:

- Business service không phụ thuộc trực tiếp vào HTTP implementation.
- Có contract test cho mapping request/response.
- Có thể chạy toàn bộ Checkout bằng mock profile.

### M5. Checkout Preview và tính tiền

**Trạng thái: Đang thực hiện**

API đã có:

```text
POST /api/v1/checkout/sessions/{sessionId}/preview
```

Request chỉ chứa ID voucher tùy chọn và `serviceCode`; không nhận giá, phí ship, discount hoặc tổng tiền từ frontend. Luồng Preview tải Address/Voucher qua gateway server-to-server, gọi Payment/GHN quote ngoài transaction, kiểm tra fingerprint/TTL/breakdown, sau đó mới khóa Checkout Session để lưu snapshot nếu selection và address vẫn chưa đổi.

Phép tính bắt buộc:

```text
itemsSubtotal
= itemsListSubtotal - directSaleDiscount

finalTotal
= itemsSubtotal
 - productDiscount
 - orderDiscount
 + shippingFee
 - shippingDiscount
```

Quy tắc:

- Mọi giá trị tiền không âm.
- `shippingDiscount` không vượt `shippingFee`.
- Discount phân bổ xuống các dòng phải khớp tổng Order.
- Voucher tính trên giá sau direct sale.
- Quote phải còn hạn, chưa dùng và đúng fingerprint.
- Đổi địa chỉ, item, quantity, voucher ship, dịch vụ hoặc kho gửi làm quote cũ mất hiệu lực.
- Backend chỉ gán `NOT_REQUIRED + FREE` khi `finalTotal = 0`.
- Từ chối `PREPAID + COD` và không cho client tự chọn `FREE`.

Tiêu chí hoàn thành:

- Có unit test đầy đủ cho công thức tiền và các biên số học.
- Preview không tạo reservation.
- Frontend không thể sửa phí hoặc tổng tiền bằng request.
- HTTP adapter thật cho Address/Voucher và fingerprint portable của Payment phải được chốt trước E2E.

### M6. Tạo Order, idempotency và Saga

**Trạng thái: Chưa bắt đầu**

API dự kiến:

```text
POST /api/v1/checkout/sessions/{sessionId}/orders
Idempotency-Key: <UUID>
```

Trình tự nghiệp vụ:

```text
Kiểm tra JWT và ownership
→ khóa/kiểm tra Checkout Session
→ kiểm tra Idempotency-Key và request hash
→ tải lại Cart hoặc Buy Now selection
→ kiểm tra lại Address
→ kiểm tra lại Variant, giá và tồn
→ validate/reserve Voucher
→ reserve Inventory
→ tạo Order và toàn bộ snapshot
→ consume Checkout Quote
→ ghi Outbox Event
→ tạo Payment nếu finalTotal > 0
→ commit/consume hoặc compensation theo payment rule
```

Yêu cầu:

- Một Checkout Session chỉ tạo tối đa một Order.
- Cùng idempotency key và cùng request trả lại cùng kết quả.
- Cùng key nhưng request khác trả conflict.
- Request đồng thời không tạo hai Order hoặc hai reservation.
- Reserve/release gửi sang service khác dùng operation key ổn định.
- Không giữ transaction database khi gọi HTTP.
- Tiến trình Saga được lưu để có thể retry hoặc compensation sau khi service restart.
- Compensation thực hiện theo thứ tự ngược và an toàn khi gọi lặp.

Tiêu chí hoàn thành:

- Saga happy path tạo đúng một Order.
- Lỗi từng bước đều bù trừ đúng.
- Restart giữa Saga không làm mất reservation hoặc tạo Order trùng.
- Snapshot không thay đổi khi dữ liệu nguồn bị sửa.

### M7. State machine và vòng đời Order

**Trạng thái: Đang thực hiện**

Luồng trả trước VNPay:

```text
PENDING_PAYMENT
→ CONFIRMED
→ PACKING
→ SHIPPING
→ DELIVERED
→ COMPLETED
```

Luồng trả sau COD/VNPay:

```text
CONFIRMED
→ PACKING
→ SHIPPING
→ HANDOVER_PENDING
→ DELIVERED
→ COMPLETED
```

Luồng đơn 0 đồng:

```text
CONFIRMED
→ PACKING
→ SHIPPING
→ DELIVERED
→ COMPLETED
```

Quy tắc:

- Admin chỉ thực hiện transition được cho phép.
- Chỉ Customer sở hữu Order được xác nhận đã nhận hàng.
- Admin không được tự đặt `DELIVERED` hoặc `COMPLETED`.
- Order trả sau chỉ hoàn tất khi vừa thanh toán thành công vừa được Customer xác nhận đã nhận.
- Hai điều kiện có thể đến theo bất kỳ thứ tự nào.
- Prepaid chỉ bị `CANCELLED` khi Payment thất bại/hết hạn trong `PENDING_PAYMENT`.
- Callback đến muộn không mở lại Order.
- Mọi transition ghi `order_status_history`.

Tiêu chí hoàn thành:

- Có unit test cho mọi transition hợp lệ và không hợp lệ.
- Event lặp không ghi history hoặc hoàn tất hai lần.

### M8. API Customer và Admin

**Trạng thái: Chưa bắt đầu**

API Customer dự kiến:

```text
GET  /api/v1/orders
GET  /api/v1/orders/{orderId}
POST /api/v1/orders/{orderId}/confirm-received
```

API Admin dự kiến:

```text
GET  /api/v1/orders/admin
GET  /api/v1/orders/admin/{orderId}
POST /api/v1/orders/admin/{orderId}/confirm
POST /api/v1/orders/admin/{orderId}/pack
POST /api/v1/orders/admin/{orderId}/ship
POST /api/v1/orders/admin/{orderId}/handover
```

Lưu ý: URL cuối cùng phải được chốt với convention API chung trước khi công bố contract.

Yêu cầu:

- Danh sách phân trang.
- Filter và sort theo allow-list.
- Customer chỉ xem Order của mình.
- Admin xem snapshot, lịch sử và command hợp lệ.
- Response dùng DTO/projection, không trả entity.
- Chi tiết lịch sử sử dụng snapshot, không tải lại tên/giá/địa chỉ hiện tại từ service khác.

### M9. Outbox và RabbitMQ

**Trạng thái: Chưa bắt đầu**

Order Service nhận:

```text
PaymentSucceeded
PaymentFailed
PaymentExpired
```

Order Service phát:

```text
OrderCreated
OrderConfirmed
PaymentDue
ShipmentDelivered
OrderCancelled
OrderCompleted
```

Event envelope tối thiểu:

```text
eventId
eventType
eventVersion
producer
aggregateId
occurredAt
correlationId
payload
```

Quy tắc:

- Ghi Outbox cùng transaction với thay đổi Order.
- Publisher gửi event và cập nhật trạng thái publish.
- Retry có backoff; lỗi lâu dài đi DLQ theo convention chung.
- Consumer ghi `processed_events` để chống trùng.
- `OrderConfirmed` nguồn `CART` chứa CartItem ID, version và quantity để Cart Service dọn đúng dòng.
- `PaymentDue` chỉ dùng cho `POSTPAID + VNPAY` khi Order vào `HANDOVER_PENDING`.
- `OrderCompleted` là nguồn chuẩn cho báo cáo và review eligibility.

### M10. Kiểm thử tích hợp và hoàn thiện

**Trạng thái: Chưa bắt đầu**

Các lớp kiểm thử cần có:

1. Unit test cho phép tính tiền.
2. Unit test cho state machine.
3. Unit test Checkout expiry, ownership và cancel.
4. Unit test Saga compensation.
5. Repository test cho constraint, index và locking.
6. Controller test cho validation và mã HTTP.
7. Security test cho `401`, `403` và truy cập chéo Customer.
8. Consumer test cho event trùng, sai thứ tự và callback đến muộn.
9. Contract test cho REST/event liên service.
10. Integration test với PostgreSQL thực.
11. E2E test khi các service phụ thuộc đã sẵn sàng.

Tình huống bắt buộc:

- Cart trống.
- Variant ngừng bán hoặc hết tồn.
- Address không thuộc Customer.
- Voucher hết hạn, sai điều kiện hoặc hết quota.
- Quote sai fingerprint, hết hạn hoặc đã dùng.
- Hai request đồng thời với cùng idempotency key.
- Hai request khác key trên cùng Checkout Session.
- Lỗi sau khi reserve Voucher nhưng trước khi reserve Inventory.
- Lỗi sau khi reserve Inventory nhưng trước khi lưu Order.
- Payment callback trùng.
- Payment callback đến sau khi prepaid Order đã hết hạn.
- Customer A đọc hoặc xác nhận Order của Customer B.
- Payment success và confirm received đến theo hai thứ tự khác nhau.
- RabbitMQ dừng rồi hoạt động lại.

## 8. Ma trận tích hợp

| Service | Order Service nhận | Order Service cung cấp |
|---|---|---|
| Identity | AddressSnapshot, ownership và trạng thái Address | Không ghi Identity DB |
| Catalog | Purchasable Variant, giá, trọng lượng, reserve/commit/release inventory | Order context nếu contract cần trace |
| Cart | Selected CartItem, version, direct sale, voucher validate/reserve/consume/release | `OrderConfirmed` để dọn đúng CartItem |
| Payment | GHN Quote, `PaymentSucceeded`, `PaymentFailed`, `PaymentExpired` | `OrderPaymentContext`, `PaymentDue` |
| Engagement | Không cần dữ liệu đồng bộ cho Create Order | `ReviewEligibility`, `OrderCompleted`, `OrderCancelled` |

## 9. Phần có thể làm ngay và phần phải chờ

### Có thể làm ngay

- M1: nền service, security và cấu hình.
- M2: schema/entity/repository.
- M3: Checkout Session.
- M4: interface client và mock adapter.
- M5: Checkout Preview và tính tiền.
- M6: Idempotency/Saga bằng mock adapter.
- M7: state machine.
- M8: API Customer/Admin.
- M9: Outbox framework và consumer test.
- Phần lớn test tự động.

### Phải phối hợp hoặc chờ contract thật

- Endpoint AddressSnapshot từ Identity.
- Endpoint kiểm tra Variant và Inventory từ Catalog.
- Endpoint Cart/Voucher từ Cart Service.
- Endpoint GHN Quote và Payment từ Payment Service.
- `ShippingQuoteResponse` phải trả `requestFingerprint` canonical do Payment tạo. Bản hiện tại yêu cầu fingerprint khi consume nhưng không trả lại; Order không được tự tái tạo từ `Map.of` vì thứ tự serialization không portable giữa hai JVM.
- Catalog/Cart phải cung cấp `lengthCm`, `widthCm`, `heightCm` cùng `weightGrams` cho mỗi Variant; không dùng kích thước mặc định hoặc dữ liệu frontend.
- RabbitMQ exchange, routing key, retry và DLQ chung.
- JWT/internal service authentication convention.

Không chờ code hoàn chỉnh của service khác để bắt đầu. Dùng mock adapter theo contract đã chốt, sau đó thay bằng HTTP/event adapter thật.

## 10. Definition of Done của phần Hiếu

Phần Order Service chỉ được xem là hoàn thành khi:

- Migration chạy được từ database sạch.
- Service chạy độc lập ở đúng cổng.
- API có validation, authentication và authorization.
- Customer không truy cập được dữ liệu của Customer khác.
- Checkout Session hết hạn/hủy đúng quy tắc.
- Preview không reserve tồn hoặc voucher.
- Cùng idempotency key chỉ tạo một Order và một tập side effect.
- Một Checkout Session chỉ tạo tối đa một Order.
- Snapshot không đổi khi dữ liệu nguồn thay đổi.
- Công thức tiền và phân bổ discount khớp tuyệt đối.
- Saga bù trừ đúng một lần khi từng bước thất bại.
- State machine không cho chuyển trạng thái trái phép.
- Payment event và confirm received đến bất kỳ thứ tự nào vẫn hoàn tất đúng một lần.
- Outbox không làm mất event khi RabbitMQ tạm dừng.
- Event gửi lại không tạo side effect trùng.
- Có test happy path, validation, ownership, idempotency, state, money và failure path.
- Không có secret, dữ liệu nhạy cảm hoặc TODO P0 trong mã bàn giao.
- Contract và hướng dẫn demo được cập nhật.

## 11. Bảng theo dõi tiến độ

Quy ước:

- `[ ]` Chưa bắt đầu.
- `[-]` Đang thực hiện.
- `[x]` Hoàn thành và đã kiểm thử.
- `[!]` Bị chặn bởi contract hoặc service khác.

| Mốc | Hạng mục | Trạng thái | Ghi chú |
|---|---|---|---|
| M0 | Chốt REST/event contract | [-] | Payment/GHN có contract nền nhưng quote response còn thiếu fingerprint portable; Identity/Catalog/Cart và event chung còn chờ |
| M1 | Nền service, security, cấu hình | [x] | Startup/health đạt; 3 JWT role test + 5 security HTTP test đạt |
| M2 | Migration/entity/repository | [-] | Flyway V1→V3 + Hibernate validate đạt trên PostgreSQL sạch; còn repository constraint/locking test |
| M3 | Checkout Session CART/BUY_NOW | [-] | API create/get/update/cancel, expiry, ownership, cancel idempotent và controller test đã có; chờ Cart/Catalog HTTP contract |
| M4 | Client interface và mock adapter | [-] | Payment/GHN adapter, Address/Voucher/selection gateway đã có; HTTP adapter còn chờ contract thật |
| M5 | Preview và tính tiền | [-] | Preview API, package rule, voucher allocation, GHN quote validation và persistence đã có; chờ response fingerprint + adapter thật |
| M6 | Create Order, idempotency, Saga | [ ] | Dùng mock adapter trước |
| M7 | State machine và Payment event | [-] | State machine + 9 unit test đạt; command service/consumer chưa triển khai |
| M8 | API Customer/Admin | [ ] | Có thể làm ngay |
| M9 | Outbox và RabbitMQ | [ ] | Cần convention RabbitMQ chung |
| M10 | Integration/E2E và demo | [ ] | Chờ API thật của các service |

## 12. Thứ tự triển khai khuyến nghị

```text
M0 Chốt contract tối thiểu
→ M1 Nền service và security
→ M2 Migration/entity/repository
→ M3 Checkout Session
→ M4 Mock client
→ M5 Preview và tính tiền
→ M6 Create Order/Saga/idempotency
→ M7 State machine và Payment event
→ M8 API Order
→ M9 Outbox/RabbitMQ
→ thay mock bằng tích hợp thật
→ M10 Integration/E2E/demo
```

## 13. Cách cập nhật tài liệu

Sau mỗi pull request liên quan đến Order Service:

1. Cập nhật trạng thái mốc trong bảng tiến độ.
2. Ghi rõ phần đã hoàn thành và test đã chạy.
3. Ghi blocker nếu contract/service phụ thuộc chưa sẵn sàng.
4. Cập nhật API/event nếu contract thay đổi.
5. Không đánh dấu `[x]` nếu mã mới chỉ compile nhưng chưa có test tương ứng.

Khi bắt đầu một mốc, đổi `[ ]` thành `[-]`. Khi toàn bộ tiêu chí của mốc đạt và test thành công, đổi thành `[x]`.

### Nhật ký triển khai

#### 2026-09-23

- Đồng bộ `main` tới commit `7506e43` có Payment/GHN contract mới.
- Tạo nhánh `VanHieu/order-checkout-foundation`.
- Hoàn thành phần mã nền của M1: cổng `8084`, JWT Resource Server, role mapping, error envelope, configuration properties, `Clock` và enum nghiệp vụ.
- Thêm 3 unit test cho ánh xạ role JWT; kết quả `3/3` đạt.
- Tạo migration `V2__align_order_schema_with_current_contract.sql` theo nguyên tắc không sửa V1.
- Bổ sung đầy đủ entity/repository và lock query nền cho Checkout, Order, Saga, Outbox và idempotency.
- `mvnw -DskipTests compile` thành công với 51 source file.
- Thêm Payment/GHN client dùng `X-Internal-Api-Key`, connect/read timeout và mapping lỗi tích hợp ổn định.
- Contract test phát hiện và đã sửa lỗi ghi đè HTTP request factory.
- Thêm lõi tính tiền số nguyên có kiểm tra tràn số, discount theo dòng và giới hạn giảm phí ship; 8 unit test đạt.
- Thêm state machine cho prepaid, postpaid, đơn 0 đồng, callback trùng/đến muộn và hai thứ tự payment/received; 9 unit test đạt.
- Chuẩn hóa `contextLoads` để không phụ thuộc credentials PostgreSQL cá nhân; kiểm thử database thật được tách sang smoke test Flyway/JPA.
- Lệnh `mvnw test` toàn module đạt: 29 test, 0 failure, 0 error, 0 skipped.
- Tạo PostgreSQL 13 tạm trên cổng riêng, chạy thành công Flyway V1→V2 và Hibernate `ddl-auto=validate` trên database sạch.
- Smoke test khởi động Order Service ở cổng thử nghiệm và `/actuator/health` trả `UP`; cụm PostgreSQL tạm đã được dừng và xóa sau kiểm thử.
- Hibernate validation đã phát hiện và giúp sửa mapping `CHAR(64)` của fingerprint/hash cùng `CHAR(3)` của currency.
- Đánh dấu M1 hoàn thành sau khi 5 security HTTP test xác nhận health public, `401`, `403` và quyền CUSTOMER/ADMIN đúng route.
- M2 còn thiếu repository test cho constraint và locking nên vẫn ở trạng thái đang thực hiện.
- Hoàn thành lõi M3: `CheckoutSessionService`, DTO và `CheckoutController` cho create/get/update/cancel; selection CART/BUY_NOW chỉ được snapshot từ `CheckoutSelectionGateway`.
- Thêm fallback `CHECKOUT_SOURCE_UNAVAILABLE` để service không dùng giá hoặc tồn kho giả khi Cart/Catalog chưa có contract HTTP.
- Thêm 6 unit test cho session: snapshot CART, từ chối item client-side, mismatch BUY_NOW, expiry, payment pair và cancel idempotent không xóa snapshot.
- Thêm 3 controller test: xác thực CUSTOMER, validation envelope và chuyển JWT `sub` thành `customerId`.
- Cập nhật context smoke test với repository mock khi tắt JPA; lệnh `mvnw test` toàn module đạt **38 test, 0 failure, 0 error, 0 skipped**.
- Thêm migration V3 để snapshot `lengthCm`, `widthCm`, `heightCm`; dữ liệu cũ được để nullable và bị từ chối báo giá thay vì dùng kích thước giả.
- Thêm `ShippingPackageCalculator` dùng đúng package rule hiện tại của Payment/GHN, có kiểm tra overflow và snapshot thiếu kích thước.
- Triển khai `CheckoutPreviewService` theo hai pha: gọi Address/Voucher/Payment ngoài transaction, sau đó `CheckoutPreviewPersistenceService` khóa session và commit quote/voucher/tổng tiền.
- Preview kiểm tra stacking tối đa một voucher hàng hóa + một voucher ship, tổng phân bổ theo dòng, shipping discount, TTL và toàn bộ breakdown server-side.
- Quote cũ bị `INVALIDATED` khi Preview mới được commit; cùng quote ID được refresh an toàn và quote thuộc session khác bị từ chối.
- Backend tự gán `NOT_REQUIRED + FREE` khi `finalTotal = 0`; client không thể tự chọn cặp này.
- Phát hiện contract gap: Payment yêu cầu fingerprint khi consume quote nhưng response hiện chưa trả fingerprint canonical. Order đã yêu cầu trường này và từ chối response thiếu bằng `INVALID_SHIPPING_QUOTE_CONTRACT`.
- PATCH đổi địa chỉ lập tức invalidated quote ACTIVE, xóa voucher/tổng tiền dẫn xuất và buộc Preview lại.
- Thêm 12 test cho package calculation, Preview orchestration, transaction persistence, invalidation khi đổi địa chỉ và endpoint Preview; toàn module đạt **50 test, 0 failure, 0 error, 0 skipped**.
- Tạo PostgreSQL 13 tạm, chạy thành công Flyway V1→V3, Hibernate `ddl-auto=validate`, khởi động service và xác nhận `/actuator/health` trả `UP`; cụm tạm đã được dừng và xóa.
