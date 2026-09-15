# PHẦN LÀM CHUNG — ARCHITECTURE, DATABASE, CONTRACT & INTEGRATION

## 1. Nguyên tắc
Đây là phần cả nhóm cùng chịu trách nhiệm.
Trưởng nhóm điều phối, tổng hợp và chốt.
Không tính là domain riêng.

## 2. Architecture
Chốt:
```text
API Gateway
Identity Service
Catalog Service
Cart Service
Order Service
Payment Service
Review & Reporting Service
```

Promotion gồm **giảm giá trực tiếp** và Voucher là module trong **Cart Service** và dùng `cart_db`; owner là Người 2. Giá niêm yết Variant vẫn thuộc Catalog; Cart Service chỉ sở hữu rule/campaign khuyến mãi. GHN location catalog (Tỉnh/Thành phố → Phường/Xã) và shipping quote P0 là adapter trong **Payment Service**; không tạo Shipping Service, GHN vận đơn/tracking hay logical database mới trong phạm vi hiện tại.

Business model là **B2C một doanh nghiệp bán cho nhiều customer**. Role nghiệp vụ là `CUSTOMER` và `ADMIN`; không có seller/store riêng, commission, payout hay split order theo seller.

Quy tắc:
- Frontend chỉ gọi Gateway.
- Service không đọc DB của service khác.
- REST dùng khi cần phản hồi ngay.
- RabbitMQ dùng cho event.
- Order điều phối checkout.
- Payment không update Order DB.
- Reporting không JOIN Order DB.
- Checkout và voucher mặc định bắt buộc JWT `CUSTOMER`; frontend chỉ nhận shipping fee do GHN/server tính.
- Admin User Management chỉ dành cho `ADMIN`; không trả password hash, refresh token hoặc token thô trong bất kỳ contract nào.

## 3. Database Architecture

6 logical DB:
```text
identity_db
catalog_db
cart_db
order_db
payment_db
engagement_db
```

Các bảng voucher/reservation nằm trong `cart_db`. Nếu sau này tách Promotion hoặc Shipping thành service, phải có quyết định kiến trúc mới, migration dữ liệu và cập nhật toàn bộ REST/event contract trước khi code.

Trưởng nhóm cần chốt sớm:
```text
Database ownership
Entity chính
Public ID / logical ID
Money datatype
Timestamp
Status
Logical relationship
Snapshot field
Naming
Migration convention
```

Không cần viết hết SQL ngay từ đầu.

Không cross-service foreign key.

Chi tiết schema, khóa/index, snapshot và bảng kỹ thuật nằm tại [09_DATABASE_DESIGN.md](09_DATABASE_DESIGN.md). Mỗi owner phải review phần database do mình sở hữu trước khi viết Flyway migration.

## 4. Database Design Workflow
```text
Trưởng nhóm draft
→ Domain owner review
→ Chốt logical model
→ Mọi người code theo contract/mock
→ Hoàn thiện schema/migration
→ Trưởng nhóm review
```

## 5. Contract-first

REST contract tối thiểu:
```text
ProductSummary
ProductDetail
PurchasableProduct
ProductVariant
DirectSalePrice
CartResponse
CheckoutCartItem
AddressSnapshot
CheckoutPreview
OrderResponse
OrderPaymentContext
PaymentResult
ReviewEligibility
ReportResponse
AdminUserSummary
AdminUserDetail
UserManagementAudit

BuyNowSelection P0
CheckoutSource P0
GhnLocationProvince P0
GhnLocationWard P0
LocationValidationResult P0
ProductQuestion P1
PaymentTiming
PaymentMethod

VoucherCandidate
VoucherValidationResult
VoucherReservationResult
VoucherDiscountBreakdown
```

Event:
```text
PaymentSucceeded
PaymentFailed
PaymentExpired
ShipmentDelivered
OrderCreated
OrderConfirmed
OrderCancelled
OrderCompleted
ReviewCreated
ReviewHidden

PaymentDue
ProductQuestionCreated P1
```

`PurchasableProduct` phải xác định một `variantId` ở P0; Cart, Checkout và Inventory không dùng Product chung làm đơn vị bán được. `BuyNowSelection` **P0** là lựa chọn tạm gồm đúng `variantId` và `quantity` của Checkout Session `source = BUY_NOW`, không tạo hoặc thay đổi CartItem. `CheckoutSource` gồm `CART` và `BUY_NOW`.

Guest bấm Mua ngay được giữ selection tối đa 15 phút bằng cookie HttpOnly có chữ ký (`variantId`, `quantity`, nonce, return URL), sau Login/Register mới tạo CheckoutSession thuộc Customer và luôn revalidate Catalog. Không giữ price/stock, không reserve, và cookie hết hạn/sai chữ ký bị từ chối.

`PaymentTiming` có `PREPAID`, `POSTPAID`, `NOT_REQUIRED`; `PaymentMethod` có `VNPAY`, `COD`, `FREE`. Cặp hợp lệ là `PREPAID + VNPAY`, `POSTPAID + VNPAY`, `POSTPAID + COD`, `NOT_REQUIRED + FREE`. Backend chỉ gán `NOT_REQUIRED + FREE` khi `final_total = 0`, không tạo Payment/URL ngoài; backend từ chối `PREPAID + COD` và client không được tự chọn `FREE` cho đơn có tiền. Order snapshot timing/method để reporting không suy diễn sai; Payment có tiền snapshot thêm amount để retry/IPN.

Checkout chỉ dành cho `CUSTOMER` đã đăng nhập. API Voucher mặc định chỉ trả dữ liệu sau JWT và backend tự filter eligibility. Address dùng danh mục GHN hai cấp: `province_id` và `ward_id`; Tỉnh/Thành phố và Phường/Xã phải do Payment Service trả về, chỉ `address_line` được nhập tự do. Trong Checkout, customer chọn Address đã lưu hoặc thêm Address inline; Address mới phải lưu qua Identity và tự được chọn để tái sử dụng. P0 dùng một kho gửi cố định. GHN quote chạy server-side từ Payment Service; `shipping_fee`, GHN `serviceId` và ETA phải được snapshot vào Order, không tin dữ liệu phí ship từ frontend. GHN chỉ quote fee, không tạo vận đơn/tracking/webhook trong scope hiện tại.


## 5.1. Giảm giá trực tiếp (Direct Sale) Contract

Direct Sale là giá campaign công khai trên Variant, **không cần login hoặc chọn mã**. Catalog lấy giá niêm yết từ `product_variants.price_vnd`, hỏi Promotion Engine giá sale đang hiệu lực theo `variantId`, rồi trả cho ProductSummary/ProductDetail:

```text
listPriceVnd
salePriceVnd                 null nếu không có direct sale
directSaleDiscountVnd
directSalePercent
directSaleEndsAt
directSalePromotionId        chỉ dùng trace nội bộ/snapshot
```

Product Card/Detail chỉ gạch `listPriceVnd` và hiện badge “Giảm x%” khi `salePriceVnd < listPriceVnd` và campaign còn hiệu lực. P0 không cho hai direct sale cùng hiệu lực trên một Variant. Direct sale có thể đi cùng **một Voucher hàng hóa**; Voucher tính trên giá sau direct sale. Checkout/Create Order phải query/revalidate giá sale một lần nữa, không tin giá frontend, sau đó snapshot `list_price_vnd`, `direct_sale_discount_vnd`, `unit_price_vnd`.

## 5.2. Promotion / Voucher Contract

Người 2 sở hữu Voucher Engine, nhưng contract phải được cả nhóm khóa sớm vì Checkout/Order phụ thuộc trực tiếp.

### Voucher scopes

```text
ORDER_DISCOUNT
SHIPPING_DISCOUNT
PRODUCT_DISCOUNT
CATEGORY_DISCOUNT
PRODUCT_LIST_DISCOUNT
```

### Discount method

```text
FIXED_AMOUNT
PERCENTAGE
```

### Lifecycle

```text
AVAILABLE
→ RESERVED
→ CONSUMED

RESERVED
→ RELEASED
```

### Reservation

```text
Create Order Saga
→ reserve Voucher
```

Voucher có `distribution_mode`:

```text
DEFAULT_FOR_ELIGIBLE  # tất cả customer đã login, server lọc điều kiện
ASSIGNED_ONLY         # chỉ customer được cấp row customer_vouchers mới thấy/dùng
CODE_ONLY             # chỉ hiện/sử dụng sau khi customer nhập mã hợp lệ
```

`customer_vouchers` là ví voucher cá nhân. Admin hoặc campaign cấp voucher theo account, có status/expiry riêng; không tạo row cho toàn bộ customer với voucher `DEFAULT_FOR_ELIGIBLE`.

`reserved_until`:

```text
min(payment.expires_at, voucher.ends_at)  (PREPAID)
```

### Stacking rule P0

```text
1 merchandise voucher
+
1 shipping voucher
```

Không cho nhiều Voucher hàng hóa chồng lên cùng eligible subtotal nếu chưa có rule nâng cao.

### Server-authoritative calculation

Checkout response phải có:

```text
items_list_subtotal
direct_sale_discount
items_subtotal
product_discount
order_discount
shipping_fee
shipping_discount
final_total
```

Frontend chỉ hiển thị.

## 5.2. Hủy Checkout trước khi có Order

`CheckoutSession` có trạng thái tối thiểu `ACTIVE`, `CANCELLED`, `COMPLETED`, `EXPIRED` và TTL 15 phút kể từ khi tạo.

CheckoutSession 15 phút chỉ là draft; Preview, cancel hoặc expiry session chưa reserve inventory/voucher. Reservation bắt đầu trong Saga Create Order: `POSTPAID`/`FREE` commit-consume ngay khi OrderConfirmed, còn `PREPAID` giữ tới Payment success/fail/expiry. Khi Saga lỗi hoặc prepaid fail/expiry, Order Service release reservation theo idempotency key. Hủy draft không tạo Payment, không phát `OrderCancelled`, không xóa CartItem. Sau khi Order đã tồn tại, customer không thể hủy trong phạm vi hiện tại; `CANCELLED` chỉ dành cho thanh toán trả trước thất bại/hết hạn hoặc ngoại lệ vận hành trước khi thu tiền, không tạo refund tự động.

GHN quote phải có `input_fingerprint` server-side gồm customer, địa chỉ canonical, item/quantity/weight, voucher ship, service và kho gửi. Create Order tính lại fingerprint và chỉ consume quote `ACTIVE`, chưa hết hạn; mọi thay đổi input phải quote lại.

Sau `OrderConfirmed`, Cart Service nhận command/event idempotent chỉ với source `CART`, xóa CartItem có `id`/`version`/quantity đúng snapshot. `BUY_NOW`, CartItem không chọn hoặc đã sửa trong lúc chờ payment không được xóa.

## 5.3. Thanh toán trả trước/trả sau

Tất cả Payment có tiền được tạo sau khi Order đã có snapshot tiền chính xác. Nếu `final_total = 0`, backend dùng `NOT_REQUIRED + FREE`, không tạo Payment/URL bên ngoài, xác nhận Order ngay và fulfillment theo luồng giao hàng thông thường. Mọi URL VNPay có TTL 15 phút. `PREPAID + VNPAY` chỉ fulfillment sau IPN hợp lệ; expiry phát `PaymentExpired`, hủy Order/release reservation một lần, IPN đến muộn chỉ audit. `POSTPAID + COD/VNPAY` cho phép fulfillment trước. Khi Order vào `HANDOVER_PENDING`, Payment Service phát `PaymentDue` và tạo link/QR gắn với Order, không dùng mã/tài khoản cá nhân của shipper. Nếu QR/link postpaid hết hạn, chỉ tạo lại QR/link khi Order còn `HANDOVER_PENDING`, không hủy Order. Customer đã đăng nhập xác nhận nhận hàng qua Order Service; ownership, trạng thái hợp lệ và idempotency là bắt buộc, sau đó Order Service phát `ShipmentDelivered`. Order chỉ `COMPLETED` khi đã có cả `PaymentSucceeded` lẫn `ShipmentDelivered`, bất kể thứ tự event; COD chỉ `PAID` khi đã thu tiền. Return/Refund tự động ngoài phạm vi P0/P1.


## 6. Event Envelope
```json
{
  "eventId": "uuid",
  "eventType": "OrderCompleted",
  "eventVersion": 1,
  "producer": "order-service",
  "aggregateId": "uuid",
  "occurredAt": "ISO-8601",
  "correlationId": "uuid",
  "payload": {}
}
```

## 7. API Gateway
- Routing.
- CORS.
- JWT validation cơ bản.
- Correlation ID.
- Rate limit cơ bản.
- Logging.
- Error normalization.

Không chứa business logic.

## 8. RabbitMQ
Chốt chung:
- exchange;
- queue;
- routing key;
- retry;
- DLQ;
- event version.

Đề xuất:
```text
dynamicmart.events
```

## 9. Outbox Convention
```text
Business Update
+ Insert Outbox
= One local transaction
```

## 10. Idempotency
Bắt buộc:
```text
Create Order
Payment IPN
Inventory Reservation
RabbitMQ Consumer
```

Consumer dùng processed_events.

## 11. Docker Compose
```text
frontend
api-gateway
identity-service
catalog-service
cart-service
order-service
payment-service
review-reporting-service
postgres
rabbitmq
redis
```

## 12. Git
```text
main
develop
feature/<domain>-<task>
fix/<domain>-<task>
docs/<task>
```

PR ghi rõ:
- domain;
- API/event changed?;
- DB proposal changed?;
- test;
- UI screenshot nếu có.

## 13. Frontend Foundation
Chốt chung:
- layout;
- API client;
- auth state;
- error/loading;
- toast;
- modal;
- form;
- pagination;
- table;
- status badge.

Luật cấu trúc feature, quyền sở hữu folder và lớp API frontend nằm tại [12_FRONTEND_CODING_RULES.md](12_FRONTEND_CODING_RULES.md). Luật đa tầng DTO/mapper/repository/service, transaction, Outbox và test backend nằm tại [11_BACKEND_CODING_RULES.md](11_BACKEND_CODING_RULES.md).

## 14. Integration Pair
```text
Người 1 + Người 2: Product → Cart
Người 1 + Người 3: Catalog/Inventory → Order
Người 2 + Người 3: Cart/Address/Promotion → Checkout
Người 3 + Người 4: Order → Payment
Người 3 + Người 5: Order → Review/Reporting
Người 1 + Người 5: Review → Product Rating
```

## 15. E2E
```text
Register/Login
→ Browse Product
→ Dynamic Filter
→ Add Cart
→ Chọn Voucher hàng hóa / freeship
→ Checkout
→ Reserve Voucher
→ Reserve Inventory
→ Create Order
→ COD/VNPay
→ Confirm
→ Consume Voucher
→ Complete
→ Review
→ Reporting
```

## 16. Failure Scenarios
- Stock=1, hai khách mua -> chỉ một reserve thành công.
- VNPay duplicate callback -> one business effect.
- RabbitMQ down -> Outbox PENDING -> publish khi broker lên.
- Payment fail -> cancel -> release inventory + release Voucher.
- Payment URL prepaid hết hạn sau 15 phút -> `PaymentExpired`, hủy/release đúng một lần; IPN đến muộn chỉ audit.
- Customer hủy CheckoutSession trước Create Order -> không có Order/Payment; inventory và voucher reservation release đúng một lần.
- CheckoutSession hết hạn sau 15 phút -> không có Order/Payment/reservation; chỉ đóng draft.
- Saga Create Order lỗi hoặc prepaid payment hết hạn -> inventory và voucher reservation release đúng một lần.
- Quote GHN bị sửa address/item/voucher/service hoặc fingerprint khác -> Create Order từ chối, buộc quote lại.
- OrderConfirmed từ Cart dọn đúng một lần các CartItem snapshot chưa đổi; Buy Now/payment failed không làm mất Cart.
- Voucher reservation hết hạn -> release idempotent.
- Voucher usage limit hết -> Checkout không được consume vượt giới hạn.
- Voucher đã consume cho Order xác nhận thì giao vận thất bại do ngoại lệ vận hành cũng không trả quota; không được dùng lại mã chỉ vì đơn không giao thành công.
- Freeship Voucher -> chỉ giảm shipping fee, không giảm product subtotal.
- GHN quote lỗi/timeout hoặc địa chỉ không map được -> không Create Order, không dùng fee frontend/fallback.
- Customer A truy cập Order B -> deny.

## 17. Trưởng nhóm điều phối
- Khóa architecture.
- Tổng hợp Database Design.
- Tổng hợp API Contract.
- Tổng hợp Event Catalog.
- Review thay đổi cross-domain.
- Điều phối integration.
- Quản lý P0/P1/P2.
- Đảm bảo E2E.
- Tổng hợp tài liệu/demo.
