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

Promotion/Voucher là module trong **Cart Service** và dùng `cart_db`; owner là Người 2. Shipping P1 là adapter trong **Payment Service**, không thêm Promotion Service, Shipping Service hay logical database mới trong phạm vi hiện tại.

Quy tắc:
- Frontend chỉ gọi Gateway.
- Service không đọc DB của service khác.
- REST dùng khi cần phản hồi ngay.
- RabbitMQ dùng cho event.
- Order điều phối checkout.
- Payment không update Order DB.
- Reporting không JOIN Order DB.

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
CartResponse
CheckoutCartItem
AddressSnapshot
CheckoutPreview
OrderResponse
OrderPaymentContext
PaymentResult
ReviewEligibility
ReportResponse

VoucherCandidate
VoucherValidationResult
VoucherReservationResult
VoucherDiscountBreakdown
```

Event:
```text
PaymentSucceeded
PaymentFailed
OrderCreated
OrderConfirmed
OrderCancelled
OrderCompleted
OrderRefunded
ReviewCreated
ReviewHidden
```


## 5.1. Promotion / Voucher Contract

Người 2 sở hữu Voucher Engine, nhưng contract phải được cả nhóm khóa sớm vì Checkout/Order phụ thuộc trực tiếp.

### Voucher scopes

```text
ORDER_DISCOUNT
SHIPPING_DISCOUNT
PRODUCT_DISCOUNT
CATEGORY_DISCOUNT
SELLER_DISCOUNT
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
CheckoutSession
→ reserve Voucher
```

`reserved_until`:

```text
min(checkout_session.expires_at, voucher.ends_at)
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
items_subtotal
product_discount
order_discount
shipping_fee
shipping_discount
final_total
```

Frontend chỉ hiển thị.


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
- Voucher reservation hết hạn -> release idempotent.
- Voucher usage limit hết -> Checkout không được consume vượt giới hạn.
- Freeship Voucher -> chỉ giảm shipping fee, không giảm product subtotal.
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
