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
- 208 automated test chạy mặc định: security, JWT role, payment/Identity/Cart/Catalog contract, pricing, Checkout Session/Preview,
  Create Order API/orchestration/idempotency/revalidation/reservation/snapshot/compensation, Payment creation/checkpoint,
  reservation finalization, recovery, lifecycle command, query API, outbox, messaging test binder,
  HTTP reservation adapter, compatibility facade, state machine và context smoke test.
- Một PostgreSQL smoke test opt-in kiểm tra Flyway và Hibernate schema validation trên database thật.
- Một PostgreSQL repository integration suite opt-in kiểm tra unique constraint, pessimistic lock và
  `FOR UPDATE SKIP LOCKED`; **6/6 test đã đạt trên PostgreSQL test tạm**, đồng thời suite được chặn an toàn để
  chỉ chạy trên database có chữ `test` trong tên.

Tình trạng xác minh bằng hạ tầng thật:

- E2E adapter thật với Identity, Catalog và Cart; HTTP adapter đã có nhưng còn cần contract/integration test đầy đủ.
- E2E liên service với Identity, Catalog, Cart và Payment thật.
- Smoke RabbitMQ broker thật đã đạt với provision exchange/queue/binding, outbound route, event lặp, poison DLQ và phục hồi sau mất broker.

Runbook kiểm thử và demo đã được tách tại `docs/ORDER_SERVICE_RUNBOOK.md`; các bước E2E liên service còn lại
giữ trạng thái chờ cho đến khi đủ service phụ thuộc và Payment hoàn thiện contract fingerprint.

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

- Tạo CART/BUY_NOW đã được kiểm thử với mock gateway; HTTP adapter thật đã được đồng bộ từ `main` và còn cần E2E.
- Session hết hạn không thể update/hủy và sẽ chuyển `EXPIRED`; Create Order API đã có ở M6.
- Hủy lặp không xóa snapshot hoặc gọi Cart/Catalog.
- Ownership được enforced trong service theo `sessionId + customerId`; còn cần controller/security test cho truy cập chéo thực tế.

### M4. Client tích hợp và mock adapter

**Trạng thái: Hoàn thành trong phạm vi Order Service; E2E được theo dõi ở M10**

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
- HTTP adapter thật cho Address/Voucher/Catalog đã có; fingerprint portable của Payment vẫn phải được Hưng bổ sung vào quote response trước E2E.

### M6. Tạo Order, idempotency và Saga

**Trạng thái: Đang thực hiện**

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

Đã triển khai bước admission đầu tiên:

- Migration V4 thêm `idempotency_key`, `request_hash` và unique index vào `order_sagas`.
- Khóa pessimistic Checkout Session trước khi bắt đầu Saga.
- Kiểm tra ownership, TTL, trạng thái, Address, Preview totals, payment pair và quote `ACTIVE` còn hạn.
- Cùng key/cùng request trả lại Saga hiện có; cùng key/request khác hoặc hai key trên cùng session bị từ chối.
- Admission được commit trước mọi lời gọi reserve bên ngoài; chưa mở endpoint Create Order cho đến khi orchestrator có thể chạy/compensate an toàn.
- 7 unit test admission đạt; Flyway V1→V4 và Hibernate validate đạt trên PostgreSQL thật.

Đã triển khai bước đóng băng và revalidation trước reservation:

- PATCH/DELETE Checkout Session dùng cùng pessimistic lock với admission và bị từ chối bằng `409 ORDER_CREATION_IN_PROGRESS` ngay khi Saga đã tồn tại; vì vậy selection không thể đổi sau khi Create Order bắt đầu.
- `OrderCreationContextReader` tải Saga, Session, Preview, Quote, item và voucher snapshot trong một transaction đọc ngắn rồi trả về context bất biến.
- `OrderCreationRevalidationService` gọi lại Cart/Buy Now, Address và Voucher ngoài transaction database; frontend không cung cấp giá, phí, địa chỉ chi tiết hoặc tổng tiền làm nguồn sự thật.
- Revalidation so sánh variant/source version, giá và khuyến mại, số lượng, kích thước/trọng lượng, địa chỉ, quyền sở hữu voucher, phân bổ giảm giá, toàn bộ money breakdown, quote fingerprint, payable amount và package metrics.
- Có 11 test mới cho việc đóng băng Checkout, đọc context và phát hiện selection/address/voucher/total/quote thay đổi; cộng với 7 test admission, phần M6 hiện có 18 test trực tiếp.
- Toàn module đạt **68 test, 0 failure, 0 error, 0 skipped** và đóng gói JAR thành công.

Đã triển khai contract reservation và checkpoint Saga:

- `VoucherReservationGateway` và `InventoryReservationGateway` định nghĩa reserve/release theo batch; mỗi request mang `sagaId`, `correlationId` và operation key ổn định để service sở hữu tài nguyên xử lý idempotent.
- Fallback mặc định trả `503 RESERVATION_SOURCE_UNAVAILABLE`; chưa giả lập reserve thành công khi Cart/Catalog chưa cung cấp HTTP contract thật.
- `OrderSagaCheckpointService` khóa pessimistic Saga và ghi từng mốc `VOUCHER_RESERVED`, `INVENTORY_RESERVED`, `COMPENSATING`, `COMPENSATED` trong transaction local ngắn, đồng thời chống retry trả reservation ID khác.
- `OrderReservationService` không mở transaction database khi gọi service ngoài; khi lỗi sau reserve, release theo thứ tự ngược Inventory → Voucher và giữ Saga ở `COMPENSATING` nếu compensation cần retry.
- Operation key được dẫn xuất tất định từ Saga + loại thao tác, nên retry/restart không tạo reservation hoặc release trùng nếu service đích tuân thủ contract idempotency.
- Thêm 12 test cho checkpoint, replay/mismatch, happy path, lỗi từng bước, thứ tự compensation và compensation failure; toàn module đạt **80 test, 0 failure, 0 error, 0 skipped** và đóng gói JAR thành công.

Đã triển khai transaction tạo Order và snapshot bất biến:

- Preview persistence dùng cùng Checkout row lock và từ chối `ORDER_CREATION_IN_PROGRESS`; sau admission, cả PATCH, DELETE và Preview đều không thể thay đổi snapshot.
- Voucher contract bổ sung `discountMethod`, `discountValue`, `eligibleSubtotalVnd` để Order lưu lịch sử chính xác thay vì suy đoán từ số tiền giảm cuối cùng.
- `OrderCreationPersistenceService` khóa Checkout, Saga và Checkout Quote; kiểm tra reservation ID khớp checkpoint `INVENTORY_RESERVED` trước khi ghi dữ liệu.
- Một local transaction tạo `CustomerOrder`, item/address/voucher/shipping snapshot, initial status history và `OrderCreated` Outbox; đồng thời consume local quote, complete Checkout và chuyển Saga sang `ORDER_CREATED`.
- Trạng thái ban đầu dùng chung `OrderStateMachine`: prepaid là `PENDING_PAYMENT`, postpaid/đơn miễn phí là `CONFIRMED`.
- Retry sau khi transaction đã commit trả lại Order hiện có và không ghi snapshot/Outbox lần hai.
- Thêm 6 test cho Preview freeze, happy path snapshot, initial status, replay, reservation mismatch và stale quote; toàn module đạt **86 test, 0 failure, 0 error, 0 skipped** và đóng gói JAR thành công.

Đã triển khai Create Order orchestrator và API:

- Mở `POST /api/v1/checkout/sessions/{sessionId}/orders`; `Idempotency-Key` là UUID bắt buộc, Customer lấy từ JWT và endpoint không nhận giá/fee/address detail từ frontend.
- `OrderCreationOrchestrator` ghép admission → replay lookup → revalidation → Voucher/Inventory reservation → remote shipping quote consume → local Order persistence mà không giữ transaction DB qua lời gọi HTTP.
- `ShippingQuoteConsumptionService` gửi customer + fingerprint authoritative sang Payment và so khớp quote ID, fingerprint, fee/discount/payable, service và expiry trước khi cho phép tạo Order.
- Lỗi consume quote sau reservation kích hoạt compensation; lỗi local persistence giữ reservation/checkpoint để cùng idempotency key retry thay vì release tài nguyên khi trạng thái commit còn chưa chắc chắn.
- Retry từ `STARTED`, `VOUCHER_RESERVED` hoặc `INVENTORY_RESERVED` chạy lại bằng operation key ổn định; retry khi Order đã commit trả `200` và không lặp remote call, request mới trả `201`.
- Thêm 12 test cho controller/header/status code, orchestrator happy path/replay/failure boundary, quote consume contract và resume từ reservation checkpoint; toàn module đạt **98 test, 0 failure, 0 error, 0 skipped** và đóng gói JAR thành công.

Đã nối Payment creation và checkpoint sau `ORDER_CREATED`:

- Migration V5 thêm `payment_id` cùng unique index có điều kiện vào `order_sagas`, cho phép Saga lưu định danh Payment bền vững và chống hai Saga trỏ đến cùng Payment.
- `OrderPaymentCreationService` gọi `PaymentClient.createPayment` sau khi transaction tạo Order đã commit; không giữ transaction database trong lúc gọi Payment.
- Response của Payment được kiểm tra chặt `paymentId`, `orderId`, amount, payment timing, payment method và status trước khi chấp nhận.
- `OrderPaymentCheckpointService` khóa Saga và Order, sau đó ghi nguyên tử `payment_id`, Saga `PAYMENT_REQUESTED`, bước `PAYMENT_CONTEXT_CREATED` và `paymentDueAt` của Order.
- Retry tại `PAYMENT_REQUESTED` trả checkpoint đã lưu và không gọi Payment lần nữa. Contract tích hợp vẫn yêu cầu Payment tạo context idempotent theo `orderId` để bao phủ trường hợp remote đã tạo thành công nhưng Order chưa kịp lưu checkpoint.
- Đơn có tổng tiền bằng 0 không gọi Payment; Saga ghi bước `PAYMENT_NOT_REQUIRED` nhưng giữ `ORDER_CREATED` để còn thực hiện commit/consume reservation.
- Nếu Payment lỗi sau khi Order đã commit, không compensation Order/reservation và giữ Saga ở `ORDER_CREATED`; retry cùng `Idempotency-Key` sẽ thử tạo Payment lại an toàn.
- Create Order response trả thêm `paymentId` và `paymentDueAt` để client tiếp tục luồng thanh toán.
- Thêm 10 test cho Payment creation/validation/retry/free-order/checkpoint và failure boundary trong orchestrator; toàn module đạt **108 test, 0 failure, 0 error, 0 skipped** qua `clean verify` và đóng gói JAR thành công.
- Smoke test Flyway V1→V5 trên PostgreSQL thật chưa chạy lại trong lượt này vì Docker Desktop không hoạt động; test opt-in đã được cập nhật để kiểm tra cột/index V5 và cần chạy khi PostgreSQL sẵn sàng.

Đã triển khai commit/consume reservation sau Payment checkpoint:

- `InventoryReservationGateway` có contract `commit`, `VoucherReservationGateway` có contract `consume`; cả hai nhận `sagaId`, `correlationId`, `orderId`, reservation ID và operation key tất định.
- `OrderReservationFinalizationService` chỉ hoàn tất reservation khi Order là `CONFIRMED`. Đơn postpaid và miễn phí được xử lý ngay trong Create Order; prepaid `PENDING_PAYMENT` giữ reservation và chờ Payment event ở M7.
- Thứ tự remote cố định là Inventory commit → Voucher consume. Service không giữ transaction database trong lúc gọi dependency và không release reservation sau khi Order đã commit.
- Saga có thêm hai trạng thái `FINALIZING_RESERVATIONS` và `INVENTORY_COMMITTED`; mỗi ranh giới remote được ghi bằng local transaction ngắn trước khi sang bước tiếp theo.
- Retry tại `FINALIZING_RESERVATIONS` gọi lại Inventory bằng cùng operation key; retry tại `INVENTORY_COMMITTED` bỏ qua Inventory và tiếp tục Voucher. Khi hoàn tất, Saga chuyển `COMPLETED` với bước `RESERVATIONS_FINALIZED`.
- Lỗi remote được lưu với đúng checkpoint cần retry và tăng `attemptCount`; lỗi ghi checkpoint được đính kèm mà không che lỗi remote gốc.
- Create Order replay có thể tiếp tục từ Payment checkpoint hoặc bất kỳ checkpoint finalization nào. HTTP adapter Catalog/Cart đã được nối vào gateway và service đích phải thực thi operation key idempotent.
- Thêm 14 test cho checkpoint, operation key, prepaid wait, postpaid/free finalize, resume từng bước và failure boundary; toàn module đạt **122 test, 0 failure, 0 error, 0 skipped** qua `clean verify` và đóng gói JAR thành công.

### M7. State machine và vòng đời Order

**Trạng thái: Hoàn thành**

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
- RabbitMQ broker smoke ngày 2026-10-03 xác nhận `PaymentSucceeded` chuyển `DELIVERED → COMPLETED`; phát lại cùng
  `eventId` vẫn giữ đúng một `processed_events`, một history và một Outbox event.

### M8. API Customer và Admin

**Trạng thái: Hoàn thành**

API Customer đã triển khai:

```text
GET  /api/v1/orders
GET  /api/v1/orders/{orderId}
GET  /api/v1/orders/{orderId}/timeline
POST /api/v1/orders/{orderId}/received
```

API Admin đã triển khai:

```text
GET  /api/v1/orders/admin
GET  /api/v1/orders/admin/{orderId}
GET  /api/v1/orders/admin/{orderId}/timeline
POST /api/v1/orders/admin/{orderId}/pack
POST /api/v1/orders/admin/{orderId}/ship
POST /api/v1/orders/admin/{orderId}/handover
```

Contract URL hiện tại tuân theo convention `/api/v1/orders`; thay đổi sau này phải được version hóa hoặc
phối hợp với consumer trước khi công bố.

Order được xác nhận tự động bởi luồng tạo đơn 0 đồng/trả sau hoặc sự kiện Payment thành công đối với
đơn trả trước, nên không cung cấp command Admin `confirm` thủ công. Cách này tránh bỏ qua checkpoint
Payment và reservation.

Yêu cầu:

- Danh sách phân trang.
- Filter và sort theo allow-list.
- Customer chỉ xem Order của mình.
- Admin xem snapshot, lịch sử và command hợp lệ.
- Response dùng DTO/projection, không trả entity.
- Chi tiết lịch sử sử dụng snapshot, không tải lại tên/giá/địa chỉ hiện tại từ service khác.

Đã triển khai:

- Danh sách Customer/Admin có phân trang, lọc `status`, `orderNumber`, khoảng `createdAt`; Admin có thêm
  `customerId`.
- Sort chỉ nhận `createdAt`, `updatedAt`, `orderNumber`, `status`, `finalTotalVnd`, tự thêm `id` làm
  tie-breaker để phân trang ổn định; giới hạn tối đa 100 bản ghi/trang.
- Customer lookup luôn gắn owner từ JWT trong database query và dùng `404 ORDER_NOT_FOUND` cho cả
  Order không tồn tại lẫn không thuộc quyền sở hữu, tránh dò IDOR.
- Detail dùng toàn bộ item/address/voucher/shipping snapshot; thiếu snapshot bắt buộc trả lỗi toàn vẹn
  rõ ràng thay vì gọi lại service khác hoặc dựng dữ liệu hiện tại.
- Timeline Customer che actor ID nội bộ và correlation kỹ thuật; Admin thấy audit đầy đủ.
- Response là DTO bất biến, có `availableActions` theo trạng thái và vai trò, không serialize JPA entity.
- Có 11 test service/controller cho ownership, RBAC, filter/sort validation, snapshot mapping, timeline
  redaction và error envelope. Toàn module đạt 174 test qua `clean verify`.

### M9. Outbox và RabbitMQ

**Trạng thái: Hoàn thành**

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

Đã triển khai:

- Consumer `paymentEvents` dùng group bền vững `order-service`, retry tối đa 5 lần.
- Sau khi hết retry, Rabbit binder republish message và thông tin lỗi vào exchange
  `dynamicmart.events.dlx`, queue `dynamicmart.events.order-service.dlq`, routing key
  `order-service.payment.failed`; message lỗi không requeue vô hạn.
- Test binder xác nhận JSON Payment được deserialize đúng contract, retry đủ 5 lần,
  `PaymentDue` đi `payment.due` và Order event dùng envelope version hóa trên `dynamicmart.events`.
- Test cấu hình bind trực tiếp vào Rabbit binder 5.0.3 để phát hiện sớm property DLQ viết sai tên.
- Outbox chuyển event sang `FAILED` sau lần gửi lỗi thứ 10; retry trước đó dùng backoff và giữ cùng event ID.

Xác minh hạ tầng thật ngày 2026-10-03:

- RabbitMQ 3.13.7 provision đúng `dynamicmart.events`, queue group `order-service`, DLX/DLQ và `payment.due`.
- Outbox thường và `PaymentDue` đều tới đúng exchange, giữ nguyên event identity và chuyển `PUBLISHED`.
- Poison JSON retry đủ 5 lần rồi vào DLQ với payload cùng metadata lỗi gốc, không requeue vô hạn.
- Khi broker bị dừng, event giữ `PENDING` với attempt/backoff; khi broker trở lại, consumer/topology tự phục hồi
  và chính event đó được publish. Mô hình vẫn là at-least-once nên consumer phải idempotent.

### M10. Kiểm thử tích hợp và hoàn thiện

**Trạng thái: Đang thực hiện**

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

Đã bổ sung trong lát cắt PostgreSQL M10:

- `DatabaseMigrationSmokeIT` kiểm tra migration/schema và chỉ được bật khi cả
  `ORDER_DATABASE_SMOKE=true` lẫn `DB_URL` trỏ tới database PostgreSQL có chữ `test` trong tên.
- `PostgresRepositoryIntegrationIT` kiểm tra khóa pessimistic tuần tự hóa hai admission cùng Checkout,
  unique Checkout/Saga, unique `Idempotency-Key` và hai recovery worker claim hai Saga khác nhau bằng
  `FOR UPDATE SKIP LOCKED`; suite cũng kiểm tra hai Outbox publisher claim hai event khác nhau.
- Hai lớp dùng Spring Cloud Stream test binder nên lần chạy database không phụ thuộc RabbitMQ thật.
- Ngày 2026-10-03, suite đã chạy trên cụm PostgreSQL 13 tạm ở cổng riêng và database
  `order_service_test`: Flyway áp thành công V1→V8, Hibernate schema validation đạt và **6/6 test đạt**.
  Không kết nối hoặc thay đổi `order_db` hiện hữu.
- Hibernate 7.4 phát cảnh báo PostgreSQL 13 thấp hơn phiên bản hỗ trợ tối thiểu 14; test hiện đạt nhưng môi trường
  triển khai nên dùng PostgreSQL 14 trở lên để nằm trong dải hỗ trợ chính thức của ORM.

Khi có database test riêng, chạy từ `services/order-service`:

```powershell
$env:ORDER_DATABASE_SMOKE='true'
$env:DB_URL='jdbc:postgresql://localhost:5432/order_service_test'
$env:DB_USERNAME='<test-user>'
$env:DB_PASSWORD='<test-password>'
.\mvnw.cmd '-Dtest=DatabaseMigrationSmokeIT,PostgresRepositoryIntegrationIT' test
```

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
| M2 | Migration/entity/repository | [x] | Flyway V1→V8, Hibernate validation và 6/6 PostgreSQL constraint/locking test đã đạt trên database test riêng |
| M3 | Checkout Session CART/BUY_NOW | [-] | API create/get/update/cancel, expiry, ownership, cancel idempotent và controller test đã có; chờ Cart/Catalog HTTP contract |
| M4 | Client interface và mock adapter | [x] | Các boundary interface, HTTP adapter, timeout/error mapping, mock fixture và contract test đã đủ; E2E liên service theo dõi ở M10 |
| M5 | Preview và tính tiền | [-] | Preview API, package rule, voucher allocation, GHN quote validation và persistence đã có; chờ response fingerprint + adapter thật |
| M6 | Create Order, idempotency, Saga | [x] | API/orchestrator, reservation/checkpoint/compensation/finalization, Payment checkpoint, recovery lease và PostgreSQL smoke V1→V8 đã đạt |
| M7 | State machine và Payment event | [x] | Payment consumer/lifecycle đã đủ; broker thật xác nhận transition và duplicate event không tạo side effect lần hai |
| M8 | API Customer/Admin | [x] | List/detail/timeline, filter/sort allow-list, ownership/IDOR, snapshot DTO, available action và command đã kiểm thử |
| M9 | Outbox và RabbitMQ | [x] | Distributed claim/lease, retry/backoff/FAILED, route thường/PaymentDue, poison DLQ và broker recovery đã đạt trên RabbitMQ thật |
| M10 | Integration/E2E và demo | [-] | 208 test mặc định + 6 PostgreSQL IT + RabbitMQ broker smoke đạt; còn E2E liên service |

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

#### 2026-09-24

- Bắt đầu M6 bằng admission transaction cho Create Order trước mọi side effect liên service.
- Thêm migration V4: Saga lưu `idempotency_key`, `request_hash` và unique index để chống trùng bền vững qua restart.
- `OrderCreationAdmissionService` khóa Checkout Session, kiểm tra ownership/TTL/Preview/payment/quote và replay đúng Saga khi retry cùng key.
- Thêm 7 unit test cho admission; toàn module đạt **57 test, 0 failure, 0 error, 0 skipped**.
- Thêm `DatabaseMigrationSmokeIT` opt-in; PostgreSQL 13 tạm chạy thành công Flyway V1→V4, Hibernate validate và kiểm tra schema idempotency; cụm tạm đã được dừng và xóa.
- Đóng băng Checkout sau admission: PATCH/DELETE cùng khóa row và từ chối khi Saga đã tồn tại, loại bỏ race với Create Order.
- Thêm `OrderCreationContextReader` để lấy context bất biến trong transaction ngắn và `OrderCreationRevalidationService` để tải lại selection, Address, Voucher rồi kiểm tra toàn bộ giá/tiền/quote/package ngoài transaction.
- Thêm 11 test cho mutation guard, context reader và revalidation; toàn module đạt **68 test, 0 failure, 0 error, 0 skipped**, `clean verify` và đóng gói JAR thành công.
- Thêm contract batch reserve/release cho Voucher và Inventory với operation key tất định; fallback an toàn trả `503` cho đến khi service sở hữu tài nguyên chốt HTTP contract.
- Thêm checkpoint Saga có pessimistic lock và `OrderReservationService` compensation theo thứ tự ngược, không giữ transaction DB trong lúc gọi service ngoài.
- Thêm 12 test cho reservation/checkpoint/compensation; toàn module đạt **80 test, 0 failure, 0 error, 0 skipped**, `clean verify` và đóng gói JAR thành công.
- Chặn cả Preview sau admission; bổ sung metadata Voucher cần thiết cho snapshot lịch sử.
- Thêm transaction tạo Order, toàn bộ snapshot, initial history và `OrderCreated` Outbox; transaction cũng consume local quote, complete Checkout và checkpoint Saga `ORDER_CREATED`.
- Thêm 6 test cho snapshot/replay/constraint logic; toàn module đạt **86 test, 0 failure, 0 error, 0 skipped**, `clean verify` và đóng gói JAR thành công.
- Mở Create Order API với `Idempotency-Key`, ghép orchestration đầy đủ đến `ORDER_CREATED` và consume remote shipping quote trước local persistence.
- Cho phép retry an toàn từ checkpoint reservation; Order đã commit được replay trực tiếp, lỗi quote được compensation còn lỗi persistence giữ tài nguyên cho retry.
- Thêm 12 test cho API/orchestrator/quote consume/resume; toàn module đạt **98 test, 0 failure, 0 error, 0 skipped**, `clean verify` và đóng gói JAR thành công.
- Nối `PaymentClient.createPayment` ngay sau checkpoint `ORDER_CREATED`, kiểm tra đầy đủ contract response rồi lưu nguyên tử `payment_id`, `PAYMENT_REQUESTED` và `paymentDueAt`.
- Retry đã có Payment checkpoint không gọi remote lần nữa; đơn miễn phí ghi `PAYMENT_NOT_REQUIRED`; lỗi Payment sau khi Order commit giữ reservation và `ORDER_CREATED` để retry thay vì compensation sai.
- Migration V5 thêm Payment checkpoint cho Saga; `DatabaseMigrationSmokeIT` đã kiểm tra thêm cột và unique index nhưng chưa thể chạy PostgreSQL smoke vì Docker Desktop không hoạt động.
- Thêm 10 test Payment creation/checkpoint và failure boundary; toàn module đạt **108 test, 0 failure, 0 error, 0 skipped**, `clean verify` và đóng gói JAR thành công.

#### 2026-09-30

- Mở rộng reservation gateway với Inventory `commit` và Voucher `consume`, dùng operation key tất định theo Saga và loại thao tác.
- Thêm checkpoint `FINALIZING_RESERVATIONS` → `INVENTORY_COMMITTED` → `COMPLETED`, ghi lỗi/attempt để retry đúng remote boundary.
- Postpaid và đơn miễn phí hoàn tất reservation ngay sau Payment checkpoint; prepaid giữ reservation trong `PENDING_PAYMENT` và chờ M7 gọi cùng finalizer sau `PaymentSucceeded`.
- Create Order replay tiếp tục an toàn từ checkpoint finalization; không compensation tài nguyên của Order đã commit.
- Thêm 14 test cho finalization và failure boundary; toàn module đạt **122 test, 0 failure, 0 error, 0 skipped**, `clean verify` và đóng gói JAR thành công.
- Fetch và merge `origin/main` tới `8c8307d` ở chế độ chưa commit; khi giải quyết chồng lấn, kiến trúc `order-service` của Hiếu được ưu tiên và contract/adapter của thành viên khác được ghép chọn lọc.
- Giữ một nguồn Create Order duy nhất là `OrderCreationOrchestrator`; endpoint one-shot từ `main` trở thành compatibility facade và chỉ ủy quyền vào orchestrator, không tự tạo Payment/finalize lần hai.
- Xóa finalization gateway không checkpoint bị trùng, nối Catalog commit và Cart Voucher consume trực tiếp vào gateway có checkpoint của Saga.
- Giải quyết collision Flyway bằng V5 cho additional payment methods của `main` và V6 cho `payment_id` checkpoint của Saga.
- Thêm 4 test cho HTTP commit/consume contract và compatibility facade/replay; toàn module đạt **126 test, 0 failure, 0 error, 0 skipped** qua `clean verify` và đóng gói JAR thành công.
- Kiểm tra chéo mã mới từ `main`: Catalog Service đạt **14/14 test**, Payment Service đạt **15/15 test**.
- Cart Service đạt 14 business test và Identity Service đạt 8 business test; mỗi module chỉ lỗi `contextLoads` do test environment truyền nguyên chuỗi `${DB_USERNAME}` cho PostgreSQL. Đây là vấn đề cấu hình test thuộc service tương ứng, không sửa chéo trong phạm vi Order Service.
- Full E2E Checkout Preview vẫn bị chặn ở contract do `ShippingQuoteResponse` thực tế của Payment Service chưa trả `requestFingerprint` mà Order cần để consume quote an toàn. Phần sửa response thuộc chủ sở hữu Payment Service; Order tiếp tục fail-fast bằng `INVALID_SHIPPING_QUOTE_CONTRACT`.
- Chưa chạy lại smoke migration PostgreSQL V1→V6 vì Docker daemon không hoạt động; kiểm tra tĩnh xác nhận các version Flyway không trùng nhau.
- Trạng thái hiện tại là merge đã giải quyết hết conflict nhưng cố ý **chưa commit/chưa push**; stash dự phòng trước merge vẫn được giữ để có thể khôi phục.
- Triển khai recovery worker cho Create Order Saga: chỉ nhận Saga đã stale, xử lý theo batch, backoff sau lỗi và tiếp tục qua chính orchestrator idempotent hiện có thay vì tạo luồng nghiệp vụ thứ hai.
- Migration V7 bổ sung recovery lease dùng `FOR UPDATE SKIP LOCKED`, cho phép nhiều instance chia việc mà không cùng claim một Saga; prepaid `PENDING_PAYMENT` không bị polling như một lỗi vì đang chờ Payment event hợp lệ.
- Recovery riêng cho `COMPENSATING` tiếp tục release Inventory → Voucher bằng operation key cũ, nên restart giữa compensation không làm mất checkpoint hoặc đổi idempotency key.
- V7 đồng thời sửa CHECK constraint `order_sagas.status` vốn chưa chứa `FINALIZING_RESERVATIONS` và `INVENTORY_COMMITTED`; nếu không sửa, PostgreSQL thật sẽ từ chối các checkpoint finalization dù unit test dùng mock vẫn đạt.
- Thêm 9 test cho lease ownership/backoff, không đếm trùng attempt, compensation recovery và worker routing/failure isolation; toàn module đạt **135 test, 0 failure, 0 error, 0 skipped**. M6 đã đủ mã recovery nhưng vẫn giữ trạng thái đang thực hiện đến khi PostgreSQL smoke xác nhận migration/native claim query V1→V7.

#### 2026-10-02

- Đồng bộ remote và xác nhận `origin/main` chỉ có thêm commit ignore workspace, không có thay đổi nghiệp vụ cần merge vào nhánh Hiếu.
- Bắt đầu lát cắt M7 bằng RabbitMQ consumer `paymentEvents` trên `dynamicmart.events`, dùng group riêng `order-service`.
- Thêm contract envelope version 1 cho `PaymentSucceeded`, `PaymentFailed`, `PaymentExpired`; chỉ xử lý event từ `payment-service`, bỏ qua event không thuộc Payment trên destination dùng chung.
- Mỗi event khóa Order/Saga, đối chiếu `paymentId`, `orderId`, amount, timing, method và correlation trước khi thay đổi dữ liệu; `processed_events` chặn tác động lặp và phát hiện tái sử dụng `eventId` sai metadata.
- `PaymentSucceeded` trả trước chuyển Order sang `CONFIRMED` rồi tiếp tục finalization reservation ngoài local transaction; retry event tiếp tục checkpoint còn dở mà không lặp state transition.
- `PaymentFailed`/`PaymentExpired` trả trước chuyển Order sang `CANCELLED`, ghi history/outbox và đưa Saga vào `COMPENSATING`; release Inventory/Voucher tiếp tục qua operation key cũ.
- `PaymentSucceeded` trả sau đến sau `DELIVERED` chuyển Order sang `COMPLETED` và ghi `OrderCompleted` vào Outbox.
- Thêm 9 test cho event validation, idempotency, transition, follow-up remote boundary và shared destination filtering; toàn module đạt **144 test, 0 failure, 0 error, 0 skipped** qua `clean verify`, đóng gói JAR thành công.
- Chưa commit/push. RabbitMQ broker integration test, `OrderConfirmed` và API đọc danh sách/chi tiết/timeline vẫn là phần tiếp theo của M7/M8/M9.
- Thêm `OrderLifecycleCommandService` và API idempotent cho admin `pack`, `ship`, `handover`; mọi transition khóa Order, đi qua state machine và ghi timeline với actor/correlation rõ ràng.
- Admin chỉ phát `PaymentDue` khi Order trả sau bằng VNPay chuyển sang `HANDOVER_PENDING`; Customer là actor duy nhất được xác nhận nhận hàng.
- Customer confirm-received kiểm tra ownership trực tiếp trong lock query, phát `ShipmentDelivered`, và chuyển tiếp `COMPLETED`/phát `OrderCompleted` trong cùng transaction khi điều kiện thanh toán đã đạt.
- `order_operation_log` lưu response theo `Idempotency-Key`; retry cùng command trả response đã lưu, còn dùng lại key cho Order/command khác bị từ chối.
- Thêm 11 test cho command, idempotency, timeline/outbox, ownership và phân quyền HTTP CUSTOMER/ADMIN. Toàn module đạt **155 test, 0 failure, 0 error, 0 skipped** qua `clean verify`, đóng gói JAR thành công.
- Nối `OrderConfirmed` vào đúng checkpoint finalization: Inventory commit và Voucher consume thành công trước, sau đó mới enqueue Cart cleanup; `BUY_NOW` không phát lệnh dọn Cart.
- Payload `OrderConfirmed` dùng đúng contract Cart hiện có gồm `eventId`, `correlationId`, `customerId`, `source` và từng CartItem `id/quantity/version`; Cart tự bỏ qua dòng đã bị sửa và chống xử lý trùng bằng `processed_events`.
- Thêm Outbox publisher có retry/backoff và giới hạn 10 lần: `OrderConfirmed` gọi internal Cart contract idempotent, `PaymentDue` gửi destination `payment.due`, các Order event còn lại gửi envelope chuẩn vào `dynamicmart.events`.
- Nếu tiến trình dừng sau khi enqueue nhưng trước khi đánh dấu Saga `COMPLETED`, deterministic operation checkpoint giúp retry dùng lại cùng event, không tạo cleanup lần hai.
- Thêm 8 test cho enqueue/replay/BUY_NOW/invalid Cart snapshot và routing/backoff Outbox; toàn module đạt **163 test, 0 failure, 0 error, 0 skipped** qua `clean verify`, đóng gói JAR thành công.
- Hoàn thành M8 với API list/detail/timeline cho Customer và Admin; Customer ownership nằm trong database query, Admin có filter `customerId`, mọi list có phân trang và sort allow-list ổn định.
- Detail chỉ đọc snapshot Order đã đóng băng, trả `availableActions` theo role/trạng thái; timeline Customer che ID actor nội bộ và correlation kỹ thuật, Admin giữ audit đầy đủ.
- Bổ sung error envelope ổn định cho path/query sai kiểu và 11 test M8; toàn module đạt **174 test, 0 failure, 0 error, 0 skipped** qua `clean verify`, đóng gói JAR thành công.
- Chưa commit/push; `.idea/compiler.xml` tiếp tục được giữ ngoài phạm vi thay đổi dự kiến commit.
- Cấu hình Rabbit consumer retry tối đa 5 lần rồi republish sang DLQ riêng; vô hiệu requeue vô hạn để
  poison message không khóa luồng Payment event.
- Thêm integration test bằng Spring Cloud Stream test binder cho deserialize Payment event, retry policy,
  route `PaymentDue` và shared Order envelope; thêm test bind YAML vào đúng Rabbit binder contract và
  terminal `FAILED` của Outbox sau lần lỗi thứ 10.
- Toàn module đạt **180 test, 0 failure, 0 error, 0 skipped** qua `clean verify`, đóng gói JAR thành công.
- Docker daemon và RabbitMQ local chưa hoạt động; PostgreSQL cổng 5432 đang mở nhưng không có bộ
  credential/test database riêng trong workspace, nên chưa chạy migration smoke lên database hiện có để
  tránh tác động nhầm dữ liệu ngoài phạm vi.
- Bổ sung `PostgresRepositoryIntegrationIT` cho năm bất biến ở tầng database: cùng Checkout bị tuần tự hóa
  bởi pessimistic lock, mỗi Checkout chỉ có một Saga, `Idempotency-Key` không được tái dùng cho Checkout
  khác, recovery worker bỏ qua Saga đang khóa và Outbox publisher bỏ qua event đang khóa bằng
  `FOR UPDATE SKIP LOCKED`.
- Siết an toàn cho cả migration smoke và repository IT: chỉ khởi động khi `ORDER_DATABASE_SMOKE=true` và
  `DB_URL` có tên database chứa `test`; sau kết nối vẫn kiểm tra lại `current_database()` như lớp phòng vệ hai.
- Cô lập hai PostgreSQL IT khỏi RabbitMQ bằng Spring Cloud Stream test binder. `test-compile` đạt với 42 test
  source; `clean verify` mặc định tiếp tục đạt **180 test, 0 failure, 0 error, 0 skipped** và đóng gói JAR thành công.
- Gọi đích danh hai lớp IT khi chưa bật biến môi trường cho kết quả **6/6 test bị skip trước khi Spring context
  khởi động**, xác nhận guard không đụng database mặc định.
- Chưa chạy năm repository test trên PostgreSQL thật vì chưa có credential/database test riêng; thay đổi lát
  cắt này vẫn chưa commit.
- Bổ sung 10 HTTP contract test cho Identity Address, Cart Checkout Selection, Catalog Variant validation và
  Cart `OrderConfirmed`; kiểm tra cả internal API key, URL, payload, mapping response và lỗi dependency.
- Sửa Checkout Selection adapter không còn tự thay trọng lượng/kích thước thiếu bằng `1`; dữ liệu giao hàng
  thiếu/sai giờ fail-fast `502 CHECKOUT_SOURCE_CONTRACT_INVALID`, đúng nguyên tắc không tạo snapshot giả.
- Adapter kiểm tra `cartId`, CartItem ID/version, Variant ID duy nhất, tập Variant trả về và giữ thứ tự dòng Cart
  kể cả khi Catalog trả response khác thứ tự. Lỗi mạng Catalog/Cart là `503`, tách khỏi lỗi nghiệp vụ `422`.
- Address adapter kiểm tra ID và toàn bộ trường snapshot bắt buộc; phân biệt địa chỉ không hợp lệ `422`, Identity
  lỗi/chậm `503` và response sai contract `502`.
- `OrderConfirmed` contract test xác nhận gửi CartItem ID/version và phản hồi rỗng không được coi là thành công,
  nhờ đó Outbox còn retry.
- Bổ sung 5 contract test còn thiếu cho Inventory reserve/release và Voucher preview/reserve/release; xác nhận
  `Idempotency-Key`, operation/Saga identity, thời hạn giữ chỗ, payload tiền và mapping reservation ID khớp
  controller/DTO hiện tại của Catalog và Cart.
- `clean verify` đạt **195 test, 0 failure, 0 error, 0 skipped** và đóng gói JAR thành công.
- Hoàn thiện contract test Payment/GHN cho tạo Shipping Quote và VNPay attempt; request chỉ dùng package
  data server-side và giữ đúng internal API key.
- Siết `HttpVoucherGateway`: response preview thiếu danh sách hoặc reservation response thiếu/sai ID trả
  `502 CART_VOUCHER_CONTRACT_INVALID`, không còn rơi vào `NullPointerException` hoặc lưu checkpoint ID rỗng.
- Contract hiện tại của Payment Service vẫn chưa trả `requestFingerprint` trong `ShippingQuoteResponse`; đây là
  blocker E2E thuộc chủ sở hữu Payment, Order tiếp tục yêu cầu contract đúng và fail-fast thay vì tự tính fingerprint.
- M4 được đánh dấu hoàn thành trong phạm vi Order Service; phần xác minh kết nối service thật thuộc M10.
  `clean verify` đạt **199 test, 0 failure, 0 error, 0 skipped** và đóng gói JAR thành công.
- Phát hiện race khi nhiều instance cùng quét một Outbox row `PENDING`; migration V8 bổ sung
  `processing_owner`, `processing_lease_until` và partial index phục vụ claim.
- `OrderOutboxClaimService` claim event trong transaction ngắn bằng `FOR UPDATE SKIP LOCKED`, sau đó mới gửi
  HTTP/RabbitMQ ngoài transaction. Worker chỉ được finalize claim của chính mình; lease hết hạn được worker khác
  tiếp quản để không làm mất event khi instance dừng giữa chừng.
- Retry trả event về `PENDING` với backoff, lần lỗi thứ 10 chuyển `FAILED`; gửi thành công chuyển `PUBLISHED` và
  xóa lease. Crash sau khi broker nhận nhưng trước khi commit vẫn có thể phát lại, nên consumer idempotency tiếp tục
  là bắt buộc theo mô hình at-least-once.
- Bổ sung 7 unit test owner/lease/reclaim/backoff và PostgreSQL IT cho hai publisher dùng `SKIP LOCKED`.
  `clean verify` đạt **206 test, 0 failure, 0 error, 0 skipped** và đóng gói JAR thành công.
- PostgreSQL local yêu cầu mật khẩu nhưng workspace không có `.env`, DB environment hoặc `pgpass`; không thử
  credential đoán và không chạm `order_db`. Hai lớp IT hiện xác nhận **6/6 test skip an toàn** khi chưa cấu hình.
- Bổ sung `docs/ORDER_SERVICE_RUNBOOK.md` gồm cấu hình môi trường, ba mức kiểm thử, thứ tự khởi động,
  kịch bản E2E Checkout/Payment/lifecycle, truy vấn đối chiếu database, RabbitMQ/DLQ smoke, ma trận nghiệm thu,
  phân loại owner lỗi tích hợp và Definition of Done cho phần Hiếu.
- Chạy lại trên working tree hiện tại: `clean verify` đạt **206 test, 0 failure, 0 error, 0 skipped** và đóng gói
  JAR thành công; gọi đích danh hai PostgreSQL IT khi chưa cấu hình tiếp tục skip an toàn **6/6 test**.

#### 2026-10-03

- Fetch lại remote: nhánh Hiếu đang hơn `origin/main` 12 commit và chậm 1 commit chỉ sửa `.gitignore`; không có
  thay đổi Payment/Order mới cần ghép. `ShippingQuoteResponse` trên `origin/main` vẫn thiếu `requestFingerprint`.
- Bổ sung toàn bộ biến Saga recovery/Outbox lease vào `.env.example` và script
  `services/order-service/scripts/order-e2e-smoke.ps1` để tự động kiểm tra health, Checkout/Preview, công thức
  tiền, Create Order replay, xung đột key và ownership chéo khi các service phụ thuộc sẵn sàng.
- Siết contract Inventory: reserve/commit/release chỉ được checkpoint khi Catalog trả đúng `reservationId` và
  trạng thái tương ứng `RESERVED`/`COMMITTED`/`RELEASED`; response sai trả
  `502 CATALOG_RESERVATION_CONTRACT_INVALID`. Voucher preview có phần tử null cũng trả contract error rõ ràng.
- Bổ sung 2 HTTP contract test cho hai trường hợp trên; `clean verify` đạt **208 test, 0 failure, 0 error,
  0 skipped** và đóng gói JAR thành công.
- Tạo cụm PostgreSQL 13 tạm độc lập trên cổng `55432`, database `order_service_test`; Flyway áp đủ V1→V8,
  Hibernate validate thành công và **6/6 test PostgreSQL đạt**: unique Checkout/Saga, unique idempotency key,
  pessimistic Checkout lock, Saga recovery `SKIP LOCKED` và Outbox claim `SKIP LOCKED`.
- Không chạm PostgreSQL hiện hữu ở cổng 5432. Cụm tạm đã dừng; môi trường công cụ chặn thao tác xóa đệ quy nên
  thư mục tạm còn tại `%TEMP%\dynamicmart-order-pgtest-20261003-a1f3` và có thể xóa thủ công sau khi kiểm tra.
- Sau khi Docker Desktop được người dùng khởi động, chạy RabbitMQ 3.13.7 tạm trên `5673/15673`; Order Service
  health `UP`, consumer kết nối và toàn bộ exchange/queue/binding/DLX/DLQ được provision đúng.
- Outbox envelope thường tới `dynamicmart.events`; `PaymentDue` tới `payment.due`; database chuyển `PUBLISHED`
  và xóa processing lease. Poison JSON retry đủ 5 lần rồi vào DLQ với routing key
  `order-service.payment.failed`, giữ payload và metadata gốc.
- Payment event broker thật chuyển Order `DELIVERED → COMPLETED`. Phát lại cùng `eventId` không nhân đôi:
  `processed_events=1`, history `=1`, Outbox `=1`.
- Dừng broker giữa lúc có Outbox event khiến record giữ `PENDING` và tăng attempt/backoff; sau khi RabbitMQ lên
  lại, Order tự nối lại, topology tự phục hồi và event chuyển `PUBLISHED`. M7 và M9 được đánh dấu hoàn thành.
