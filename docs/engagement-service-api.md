# Engagement Service API

Owner: Tùng. Database: `engagement_db`.

Engagement Service chỉ đọc/ghi database của mình. Review eligibility, order completion và payment success đến từ contract/event của service khác; không truy vấn trực tiếp database khác.

## Security

- Public:
  - `GET /api/v1/reviews/products/{productId}`
  - `GET /api/v1/product-questions/products/{productId}`
- Customer JWT:
  - wishlist, notification, customer support, create review/question.
- Admin JWT:
  - `/api/v1/admin/**`, `/api/v1/reports/**`.
- Internal:
  - `POST /api/v1/engagement/internal/events/**`
  - Header bắt buộc: `X-Internal-Api-Key`.

## Reviews

### `GET /api/v1/reviews/products/{productId}`

Trả review `VISIBLE` theo product, phân trang bằng `page`, `size`.

### `POST /api/v1/reviews`

Customer tạo review từ OrderItem đủ điều kiện.

```json
{
  "orderItemId": "uuid",
  "rating": 5,
  "content": "San pham tot",
  "imageUrls": ["https://cdn.example/review.jpg"]
}
```

Service gọi Order Service `ReviewEligibility`; không nhận `productId`/`variantId` từ client.

### `GET /api/v1/admin/reviews`

Admin xem toàn bộ review.

### `PATCH /api/v1/admin/reviews/{reviewId}/hide`

```json
{ "reason": "Noi dung vi pham" }
```

## Wishlists

Tên bảng dùng theo database: `wishlists`, `wishlist_items`.

- `GET /api/v1/wishlists/me`
- `POST /api/v1/wishlists/me/items`
- `DELETE /api/v1/wishlists/me/items/{productId}`

```json
{ "productId": "uuid" }
```

Một product không được duplicate trong wishlist của cùng customer.

## Product Q&A

Tên bảng: `product_questions`, `product_answers`.

- `GET /api/v1/product-questions/products/{productId}`
- `GET /api/v1/product-questions/me`
- `POST /api/v1/product-questions`
- `GET /api/v1/admin/product-questions`
- `POST /api/v1/admin/product-questions/{questionId}/answers`
- `PATCH /api/v1/admin/product-questions/{questionId}/hide`

Question status: `OPEN`, `ANSWERED`, `HIDDEN`.

## Notifications

- `GET /api/v1/notifications`
- `PATCH /api/v1/notifications/{notificationId}/read`

Customer chỉ đọc notification của chính mình.

## Reporting

Reporting chỉ cập nhật từ `OrderCompleted` đã chống trùng bằng `processed_events`.

- `GET /api/v1/reports/sales/daily?from=2026-10-01&to=2026-10-05`
- `GET /api/v1/reports/products/best-sellers?from=2026-10-01&to=2026-10-05&limit=10`

Revenue P0 là tổng `finalTotalVnd` của order `COMPLETED`, chưa gọi là lợi nhuận và chưa trừ refund.

### Internal `OrderCompleted`

`POST /api/v1/engagement/internal/events/order-completed`

```json
{
  "eventId": "uuid",
  "eventType": "OrderCompleted",
  "eventVersion": 1,
  "producer": "order-service",
  "aggregateId": "order uuid",
  "occurredAt": "2026-10-05T10:00:00Z",
  "correlationId": "uuid",
  "payload": {
    "orderId": "uuid",
    "customerId": "uuid",
    "finalTotalVnd": 120000,
    "grossItemSalesVnd": 150000,
    "discountValueVnd": 30000,
    "shippingFeeVnd": 0,
    "items": [
      {
        "productId": "uuid",
        "variantId": "uuid",
        "quantity": 2,
        "grossSalesVnd": 150000,
        "netItemSalesVnd": 120000
      }
    ]
  }
}
```

### Internal `PaymentSucceeded`

`POST /api/v1/engagement/internal/events/payment-succeeded`

Chỉ tạo notification/audit, không cộng revenue.

## Chat Customer-Admin

Tên bảng: `chat_conversations`, `chat_messages`.

Customer:

- `GET /api/v1/support/conversations`
- `GET /api/v1/support/conversations/{conversationId}`
- `POST /api/v1/support/conversations`
- `POST /api/v1/support/conversations/{conversationId}/messages`

Admin:

- `GET /api/v1/admin/support/conversations`
- `GET /api/v1/admin/support/conversations/{conversationId}`
- `PATCH /api/v1/admin/support/conversations/{conversationId}/assign`
- `POST /api/v1/admin/support/conversations/{conversationId}/messages`
- `PATCH /api/v1/admin/support/conversations/{conversationId}/close`

Schema hiện tại hỗ trợ status `OPEN`, `CLOSED`; nếu cần status `ASSIGNED` hoặc idempotency key cho message thì tạo migration mới.
