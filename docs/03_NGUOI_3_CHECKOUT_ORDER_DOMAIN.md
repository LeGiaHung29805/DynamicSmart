# NGƯỜI 3 — CHECKOUT & ORDER DOMAIN

## 1. Phạm vi
Sở hữu end-to-end:
```text
Checkout
Order
Order Lifecycle
Saga
Compensation
```

## 2. Frontend
Customer:
- Checkout.
- Address selection.
- Checkout Preview.
- Payment method selection.
- Order History.
- Order Detail.
- Timeline.
- Cancel Order.

Seller:
- Order List.
- Order Detail.
- Confirm.
- Pack.
- Ship.
- Mark Delivered theo policy demo.

## 3. Backend
- Order Service.
- Checkout Preview.
- Create Order.
- Server-side total.
- Address Snapshot.
- OrderItem Snapshot.
- Idempotency-Key.
- State Machine.
- Order History.
- Ownership.
- Customer Cancel.
- Seller command.
- Saga.
- Compensation.
- Order Outbox.
- Payment event consumer.

## 4. Checkout Preview
```text
JWT Customer
→ Cart items
→ Address
→ Catalog price/status
→ Inventory check
→ Promotion/Voucher validation
→ Shipping rule
→ Voucher discount breakdown
→ Server total
→ Preview
```

## 5. Create Order
```text
POST /api/v1/orders
Idempotency-Key: UUID
```

Flow:
```text
Validate Idempotency
→ Cart
→ Address
→ Product validation
→ Validate/Reserve Voucher
→ Reserve Inventory
→ Create Order
→ Snapshot Items
→ Snapshot Voucher/Discount
→ Create Payment Session
```

## 6. State Machine
```text
PENDING_PAYMENT
→ CONFIRMED
→ PACKING
→ SHIPPING
→ DELIVERED
→ COMPLETED
```

Nhánh:
```text
CANCELLED
REFUNDED
```

`PAYMENT_FAILED` là trạng thái/event của Payment, không phải trạng thái Order. Khi payment thất bại, Order chuyển từ `PENDING_PAYMENT` sang `CANCELLED` và chạy compensation. Với COD, việc chuyển từ `PENDING_PAYMENT` sang `CONFIRMED` tuân theo policy xác nhận đơn; không được tự đánh dấu Payment là `PAID` trước khi thu COD.

## 7. Saga
Success:
```text
Reserve Inventory
→ Create Order
→ PaymentSucceeded
→ Confirm Order
→ Commit Inventory
```

Failure:
```text
Reserve Voucher
→ Reserve Inventory
→ Create Order
→ PaymentFailed / CheckoutExpired / Cancel
→ Cancel Order
→ Release Inventory
→ Release Voucher
```

Success:
```text
PaymentSucceeded
→ Confirm Order
→ Commit Inventory
→ Consume Voucher
```

Người 3 **không tự tính lại Promotion/Voucher**; chỉ gọi contract do Người 2 sở hữu.

## 8. Event
Consume:
```text
PaymentSucceeded
PaymentFailed
PaymentRefunded
```

Produce:
```text
OrderCreated
OrderConfirmed
OrderCancelled
OrderCompleted
OrderRefunded
```

## 9. Review Eligibility
Cho Người 5:
```text
GET /internal/v1/order-items/{orderItemId}/review-eligibility
```


## 9.1. Voucher Snapshot trong Order

Order phải lưu snapshot đủ để lịch sử đơn hàng không thay đổi khi Voucher sau này bị sửa/tắt.

Tối thiểu:

```text
voucher_code
voucher_scope
discount_method
discount_value
eligible_subtotal
discount_amount
shipping_discount_amount
```

Nếu dùng 2 nhóm Voucher:

```text
merchandise_voucher_snapshot
shipping_voucher_snapshot
```

Tổng tiền luôn do server tính.


## 10. Integration
Nhận Người 1:
```text
ProductCheckoutValidation
InventoryReservationResult
```

Nhận Người 2:
```text
CheckoutCartItem[]
AddressSnapshot
VoucherCandidate[]
VoucherValidationResult
VoucherReservationResult
VoucherDiscountBreakdown
```

Cho Người 4:
```text
OrderPaymentContext
```

Nhận Người 4:
```text
PaymentSucceeded
PaymentFailed
```

Cho Người 5:
```text
ReviewEligibility
OrderCompleted
OrderCancelled
OrderRefunded
```

## 11. Mock
Dùng mock Cart/Address/Product/Payment từ đầu.
Cung cấp OrderPaymentContext mock cho Người 4.
Cung cấp OrderCompleted event mẫu cho Người 5.

## 12. Test
- Empty Cart.
- Invalid Address ownership.
- Price changed.
- Product inactive.
- Insufficient stock.
- Fake total.
- Invalid Voucher ignored/rejected.
- Voucher wrong scope.
- Voucher reservation expired.
- Voucher already consumed.
- Voucher stacking conflict.
- Shipping Voucher only affects shipping fee.
- Discount snapshot immutable.
- Same Idempotency-Key.
- Snapshot immutable.
- Valid/invalid transition.
- Duplicate PaymentSucceeded.
- PaymentFailed.
- Cancel releases stock.
- RabbitMQ down + Outbox.
- Cross-user access denied.

## 13. Deliverable
- Checkout UI.
- Customer/Seller Order UI.
- Order Service.
- Snapshot.
- State Machine.
- Saga.
- Compensation.
- Outbox.
- Swagger.
- Unit/Integration Test.
- Fixtures.
- Demo.
- Tài liệu domain.
