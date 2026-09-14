# NGƯỜI 4 — PAYMENT & SHIPPING DOMAIN

## 1. Phạm vi
Sở hữu end-to-end:
```text
Payment
COD
VNPay
Payment Callback
Shipping Integration P1
```

## 2. Frontend
- Payment Method.
- COD.
- VNPay redirect.
- Payment Return.
- Payment Status.
- Payment Error/Retry.
- Shipping/Tracking UI P1.

## 3. Backend
- Payment Service.
- Create Payment Session.
- COD.
- VNPay Sandbox.
- Payment URL.
- Return URL.
- IPN.
- Checksum.
- Amount validation.
- Payment State Machine.
- Payment idempotency.
- Payment audit.
- Payment Outbox.
- Shipping Adapter P1.

## 4. COD
```text
Create Payment → PENDING
```
Không PAID ngay.

## 5. VNPay
Create URL:
```text
OrderPaymentContext
→ Server amount
→ Reference
→ TTL
→ Signature
→ Payment URL
```

IPN:
```text
Parse
→ Verify checksum
→ Find payment
→ Verify amount
→ Verify status
→ Audit transaction
→ Update Payment
→ Insert Outbox
→ Commit
```

## 6. Event
Produce:
```text
PaymentSucceeded
PaymentFailed
PaymentRefunded
```

## 7. Shipping P1
Có thể dùng:
```text
ShippingProvider
InternalShippingProvider
GhnShippingProvider
```

Tối thiểu:
- quote;
- fee;
- ETA;
- tracking.

## 8. Integration
Nhận Người 3:
```text
OrderPaymentContext
```

Gửi Người 3:
```text
PaymentSucceeded
PaymentFailed
PaymentRefunded
```

Gửi Người 5 nếu cần:
```text
PaymentSucceeded
```

## 9. Mock
Dùng OrderPaymentContext fixture để code độc lập.
Cung cấp Payment event fixture cho Người 3/5.

## 10. Test
COD:
- initial PENDING.
- no early PAID.
- duplicate confirmation safe.

VNPay:
- valid/invalid checksum.
- wrong amount.
- wrong reference.
- duplicate callback.
- unique provider transaction.
- already paid.

Outbox:
- broker down.
- retry.
- duplicate event safe.

Shipping P1:
- timeout.
- invalid quote.
- duplicate tracking prevention.

## 11. Deliverable
- Payment UI.
- Payment Service.
- COD.
- VNPay Sandbox.
- Return/IPN.
- Idempotency.
- Audit.
- Outbox.
- Shipping P1.
- Swagger.
- Unit/Integration Test.
- Fixtures.
- Demo.
- Tài liệu domain.
