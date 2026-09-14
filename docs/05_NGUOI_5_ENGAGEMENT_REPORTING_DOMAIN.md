# NGƯỜI 5 — ENGAGEMENT & REPORTING DOMAIN

## 1. Phạm vi
Sở hữu end-to-end:
```text
Review
Wishlist P1
Notification P1
Reporting P0/P1
Revenue & Best Seller P0
Read Model
```

## 2. Frontend
Customer:
- Review List.
- Review Form.
- Wishlist P1.
- Notification UI P1.

Admin:
- Review Moderation.
- Revenue Dashboard.
- Best Seller.
- Category Sales P1.
- Order Statistics P1.
- Refund/Net Revenue P1.

## 3. Backend
- Review & Reporting Service.
- Review API.
- Review Eligibility integration.
- Review Moderation.
- Wishlist P1.
- Event Consumer.
- processed_events.
- Revenue Read Model.
- Product Sales Summary.
- Category Sales Summary P1.
- Order Statistics P1.
- Notification P1.

## 4. Review
```text
POST /api/v1/reviews
```

Frontend chỉ gửi:
```text
orderItemId
rating
comment
```

Backend lấy Customer từ token và gọi Order để check eligibility.

## 5. Review Eligibility
```text
Customer đúng
AND Order COMPLETED
AND OrderItem thuộc Order
AND chưa review
```

## 6. Reporting
Không query trực tiếp Order DB.

Consume:
```text
OrderCompleted
OrderCancelled
OrderRefunded
PaymentSucceeded
```

Build:
```text
Revenue / Best Seller P0
Category Sales / Order Statistics / Refund-Net Revenue P1
```

`PaymentSucceeded` chỉ dùng cho notification hoặc đối soát nếu cần; không được cộng doanh thu hay best seller từ event này. Các chỉ số P0 chỉ cập nhật từ `OrderCompleted` và được đảo/điều chỉnh bằng `OrderRefunded` khi P1 được làm.

## 7. Idempotent Consumer
Mỗi event có eventId.

Duplicate:
```text
đã xử lý → ACK/no-op
```

Không được:
- tăng revenue hai lần;
- tăng best seller hai lần;
- gửi notification hai lần.

## 8. Event
Consume:
```text
OrderCompleted
OrderCancelled
OrderRefunded
PaymentSucceeded
```

Produce:
```text
ReviewCreated
ReviewHidden
```

## 9. Integration
Nhận Người 3:
```text
ReviewEligibility
OrderCompleted
OrderCancelled
OrderRefunded
```

Nhận Người 4:
```text
PaymentSucceeded
```

Gửi Người 1:
```text
ReviewCreated
ReviewHidden
```

## 10. Mock
Dùng OrderCompleted/Refunded event fixture.
Dùng ReviewEligibility fixture.

## 11. Test
Review:
- not purchased.
- not completed.
- wrong owner.
- duplicate review.
- invalid rating.

Reporting:
- completed increases revenue.
- cancelled does not.
- refund decreases net.
- duplicate completed no double count.
- best seller correct.

Notification P1:
- duplicate no double send.
- retry.
- sent no resend.

## 12. Deliverable
- Review UI.
- Wishlist P1.
- Notification UI P1.
- Admin Reporting UI.
- Review & Reporting Service.
- Review flow.
- Read Model.
- Revenue.
- Best Seller.
- processed_events.
- Swagger.
- Unit/Integration Test.
- Fixtures.
- Demo.
- Tài liệu domain.
