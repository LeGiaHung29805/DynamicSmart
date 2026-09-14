# DYNAMICMART — PHÂN CÔNG THEO DOMAIN

> **Tài liệu nguồn hiện hành:** Bộ `00_OVERVIEW.md` đến `07_CAI_DAT_PHAN_MEM.md` là phạm vi và phân công được nhóm dùng để triển khai. Nếu có mâu thuẫn với `DYNAMICMART_PHAM_VI_KIEN_TRUC_VA_PHAN_CONG.md`, ưu tiên bộ tài liệu này.

## 1. Mục tiêu
Nhóm gồm **5 thành viên** và chia việc theo **domain (miền nghiệp vụ)**.

Mỗi người chịu trách nhiệm end-to-end:
```text
Phân tích nghiệp vụ
→ Frontend của domain
→ REST API / Backend
→ Business Logic
→ Tích hợp
→ Unit Test
→ Integration Test
→ Tài liệu
→ Demo
```

Database Architecture, API/Event Contract, Gateway, RabbitMQ, Docker, Git và E2E là **phần làm chung**, không tính vào nhiệm vụ riêng.

---

## 2. Kiến trúc
```text
API Gateway

1. Identity Service
2. Catalog Service
3. Cart Service
4. Order Service
5. Payment Service
6. Review & Reporting Service
```

Promotion/Voucher là module thuộc **Cart Service** (do Người 2 sở hữu), không tách thành service/database thứ bảy ở P0. Shipping chỉ là adapter của Payment Service ở P1.

Tổng:
```text
6 Business Microservices
1 API Gateway
6 logical databases
```

Các database:
```text
identity_db
catalog_db
cart_db
order_db
payment_db
engagement_db
```

---

## 3. Phân chia 5 domain

| Người | Thành viên từ Fashion Ecommerce | Domain | Phạm vi |
|---|---|---|
| Người 1 | Tiến | Catalog & Inventory | Category, Dynamic Attribute, Product, Search/Filter, Inventory |
| Người 2 | Thảo | Customer Identity, Cart & Promotion | Auth, Customer, Address, Cart, Promotion/Voucher Engine |
| Người 3 | Hiếu | Checkout & Order | Checkout, Order, Snapshot, State Machine, Saga |
| Người 4 | Hưng | Payment & Shipping | COD, VNPay, Payment Flow, Shipping integration |
| Người 5 | Thành viên thứ 5 — cần chốt tên | Engagement & Reporting | Review, Wishlist, Notification, Reporting, Best Seller |

Chi tiết đối chiếu ownership cũ–mới, phần bàn giao và reviewer nghiệp vụ nằm trong `08_CHUYEN_GIAO_VAI_TRO_TU_FASHION_ECOMMERCE.md`.

---

## 4. Người 1 — Catalog & Inventory
Frontend:
- Product List.
- Product Detail.
- Dynamic Filter.
- Category navigation.
- Admin/Seller Category, Attribute, Product, Inventory.

Backend:
- Catalog Service.
- Dynamic Attribute validation.
- Product CRUD.
- Search/Filter.
- Inventory Reservation.
- Concurrency control.

Test:
- Attribute validation.
- Search/filter.
- Stock concurrency.
- Reserve/commit/release idempotency.

---

## 5. Người 2 — Customer Identity, Cart & Promotion
Frontend:
- Register/Login.
- Profile.
- Address Book.
- Cart.
- Voucher Wallet / Apply Voucher.
- Admin/Seller Promotion Management.

Backend:
- Identity Service.
- Cart Service.
- JWT/RBAC.
- Ownership.
- Cart APIs.
- Promotion/Voucher Engine.
- Voucher scope & eligibility.
- Voucher reservation lifecycle.
- Voucher stacking rules.

Voucher types/scopes:
```text
ORDER_DISCOUNT
SHIPPING_DISCOUNT
PRODUCT_DISCOUNT
CATEGORY_DISCOUNT
SELLER_DISCOUNT
PRODUCT_LIST_DISCOUNT
```

Discount method:
```text
FIXED_AMOUNT
PERCENTAGE
```

Lifecycle:
```text
AVAILABLE
→ RESERVED
→ CONSUMED

RESERVED
→ RELEASED
```

Test:
- Authentication.
- Authorization.
- Address ownership.
- Cart ownership.
- Quantity/duplicate item.
- Voucher scope.
- Voucher usage limit.
- Per-customer limit.
- Reserve/consume/release idempotency.
- Voucher stacking rules.

---

## 6. Người 3 — Checkout & Order
Frontend:
- Checkout.
- Order History.
- Order Detail.
- Timeline.
- Cancel Order.
- Seller Order flow.

Backend:
- Order Service.
- Checkout orchestration.
- Snapshot.
- State Machine.
- Saga.
- Compensation.
- Order Outbox.

Test:
- Checkout validation.
- Order idempotency.
- Snapshot.
- State transition.
- Compensation.
- Duplicate event.

---

## 7. Người 4 — Payment & Shipping
Frontend:
- Payment method.
- VNPay redirect.
- Payment result/status.
- Shipping/tracking nếu P1.

Backend:
- Payment Service.
- COD.
- VNPay Sandbox.
- IPN.
- Checksum/amount validation.
- Payment idempotency.
- Payment Outbox.
- Shipping Adapter P1.

Test:
- Invalid checksum.
- Wrong amount.
- Duplicate callback.
- Outbox.
- Shipping timeout P1.

---

## 8. Người 5 — Engagement & Reporting
Frontend:
- Review.
- Wishlist P1.
- Notification nếu P1.
- Admin Review Moderation.
- Revenue Dashboard.
- Best Seller.
- Statistics.

Backend:
- Review & Reporting Service.
- Review eligibility integration.
- Read Model.
- Event consumer.
- processed_events.
- Notification P1.

Test:
- Review eligibility.
- Duplicate review.
- Duplicate OrderCompleted.
- Revenue/refund.
- Best Seller.

---

## 9. Phần làm chung
Không thuộc riêng ai:
- Microservices Architecture.
- Database Architecture.
- Database ownership.
- Entity chính/ID strategy.
- API Contract.
- Internal API Contract.
- Event Contract.
- API Gateway.
- RabbitMQ.
- Outbox/processed_events convention.
- Docker Compose.
- Git Workflow.
- Frontend Foundation.
- Integration Test.
- E2E.
- Tài liệu chung.
- Demo.

---

## 10. Làm song song bằng mock
Không làm tuần tự.

Người 1 cung cấp mock Product.
Người 2 cung cấp mock Cart/Address.
Người 3 cung cấp mock OrderPaymentContext và Order events.
Người 4 cung cấp mock Payment result/events.
Người 5 dùng event fixture để làm Reporting.

Sau đó:
```text
Mock → Real REST API / Real Event
```

---

## 11. Những thứ phải khóa sớm
```text
Service boundary
Database ownership
Entity chính
ID strategy
API contract
Event contract
Order Status
Payment Status
Voucher type/scope/status
Voucher stacking rule
Money datatype
```

Có thể hoàn thiện sau:
```text
Index tối ưu
Seed đầy đủ
Constraint phụ
Query optimization
Audit field phụ
```

---

## 12. Tiêu chí hoàn thành domain
Một domain chỉ hoàn thành khi:
- Frontend chạy.
- Backend chạy.
- API documented.
- Business rule đúng.
- Test đạt.
- Mock đã thay bằng tích hợp thật.
- Có demo.
- Có tài liệu.
