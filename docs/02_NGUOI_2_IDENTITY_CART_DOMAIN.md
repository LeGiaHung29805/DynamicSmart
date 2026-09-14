# NGƯỜI 2 — CUSTOMER IDENTITY, CART & PROMOTION DOMAIN

## 1. Phạm vi
Sở hữu end-to-end:
```text
Identity
Customer
Address
Cart
Promotion / Voucher Engine
```

## 2. Frontend
- Register.
- Login.
- Logout.
- Profile.
- Address Book.
- Cart.
- Quantity update.
- Remove/select item.
- Voucher Wallet / Voucher Selector.
- Apply/Remove Voucher.
- Hiển thị Voucher đủ/không đủ điều kiện.
- Hiển thị discount breakdown.
- Admin/Seller Promotion Management.
- Auth guard phối hợp phần chung.

## 3. Backend

Identity Service:
- Register.
- Login.
- Refresh Token.
- Logout.
- JWT.
- Role.
- Profile.
- Address.
- Ownership.
- Authorization.

Cart Service:
- Get Cart.
- Add/Update/Remove CartItem.
- Select item.
- Checkout item retrieval.
- Product validation integration.

Promotion / Voucher Engine:
- Create/Update/Disable Voucher.
- Voucher eligibility.
- Voucher scope resolution.
- Discount calculation.
- Global usage limit.
- Per-customer usage limit.
- Minimum order/subtotal condition.
- Start/end time.
- Reserve Voucher.
- Consume Voucher.
- Release Voucher.
- Reservation expiry.
- Voucher stacking policy.
- Idempotency.

## 4. API
```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout

GET/PATCH /api/v1/me

GET/POST/PATCH/DELETE /api/v1/me/addresses

GET /api/v1/cart
POST /api/v1/cart/items
PATCH /api/v1/cart/items/{id}
DELETE /api/v1/cart/items/{id}

GET /api/v1/me/vouchers
POST/PATCH /api/v1/admin/vouchers

GET /internal/v1/users/{userId}/addresses/{addressId}
GET /internal/v1/carts/{customerId}/checkout-items
POST /internal/v1/vouchers/validate
POST /internal/v1/voucher-reservations
POST /internal/v1/voucher-reservations/{reservationId}/consume
POST /internal/v1/voucher-reservations/{reservationId}/release
```

Các API `internal` chỉ nhận lời gọi đã xác thực từ service; các thao tác reserve/consume/release phải nhận operation/idempotency key. Endpoint quản trị yêu cầu quyền `ADMIN` hoặc `SELLER` theo policy được chốt.

## 5. Business Rules

Identity/Cart:
- Register public chỉ tạo CUSTOMER.
- Password hash.
- Không log secret/token.
- Customer chỉ xem dữ liệu của mình.
- quantity > 0.
- Một Product chỉ một CartItem.
- Không tin price frontend.
- Checkout revalidate qua Catalog.

Promotion/Voucher:
- Frontend không được tự quyết định discount.
- Voucher code chuẩn hóa trước khi kiểm tra.
- Voucher chỉ áp dụng khi ACTIVE và nằm trong thời gian hiệu lực.
- Có thể giới hạn tổng lượt sử dụng.
- Có thể giới hạn lượt dùng trên mỗi Customer.
- Có thể yêu cầu minimum subtotal/order amount.
- Có thể giới hạn theo Product, Category, Seller hoặc danh sách Product.
- Shipping Voucher chỉ giảm phí vận chuyển.
- Product/Category/Seller Voucher chỉ tính trên eligible subtotal.
- Percentage Voucher phải hỗ trợ `max_discount_amount`.
- Voucher không được làm tổng tiền âm.

## 6. Promotion / Voucher Engine

### 6.1. Voucher Type / Scope

Hỗ trợ tối thiểu:

```text
ORDER_DISCOUNT
SHIPPING_DISCOUNT
PRODUCT_DISCOUNT
CATEGORY_DISCOUNT
SELLER_DISCOUNT
PRODUCT_LIST_DISCOUNT
```

Ý nghĩa:

```text
ORDER_DISCOUNT
→ giảm trên subtotal/order đủ điều kiện

SHIPPING_DISCOUNT
→ giảm shipping fee

PRODUCT_DISCOUNT
→ áp dụng cho một Product cụ thể

CATEGORY_DISCOUNT
→ áp dụng cho Product thuộc Category

SELLER_DISCOUNT
→ áp dụng cho Product thuộc Seller

PRODUCT_LIST_DISCOUNT
→ áp dụng cho danh sách Product được chọn
```

### 6.2. Discount Method

```text
FIXED_AMOUNT
PERCENTAGE
```

Ví dụ:

```text
FREESHIP50
scope = SHIPPING_DISCOUNT
method = FIXED_AMOUNT
value = 50000
```

```text
LAPTOP10
scope = CATEGORY_DISCOUNT
method = PERCENTAGE
value = 10
max_discount_amount = 1000000
```

### 6.3. Điều kiện Voucher

Voucher có thể có:

```text
starts_at
ends_at
minimum_order_amount
minimum_eligible_subtotal
usage_limit
usage_limit_per_customer
max_discount_amount
active
```

Tùy scope có thể liên kết tới:

```text
product_id
category_id
seller_id
product_ids[]
```

### 6.4. Voucher Lifecycle

Không chỉ validate rồi trừ tiền.

```text
AVAILABLE
   ↓
RESERVED
   ↓
CONSUMED
```

Nếu Checkout thất bại/hủy/hết hạn:

```text
RESERVED
   ↓
RELEASED
```

### 6.5. Reservation theo CheckoutSession

Khi Customer bước vào luồng tạo Order và chọn Voucher:

```text
CheckoutSession
→ validate Voucher
→ reserve Voucher
```

Reservation phải có:

```text
reservation_id
checkout_session_id
customer_id
voucher_id
reserved_until
status
```

Quy tắc:

```text
reserved_until
=
min(checkout_session.expires_at, voucher.ends_at)
```

Khi Order commit:

```text
RESERVED
→ CONSUMED
```

Khi Order/Checkout hủy hoặc hết hạn:

```text
RESERVED
→ RELEASED
```

Tác vụ release reservation hết hạn phải gọi lặp an toàn.

### 6.6. Stacking Rule

P0 khuyến nghị:

```text
tối đa 1 Voucher giảm hàng hóa
+
tối đa 1 Voucher vận chuyển
```

Nhóm giảm hàng hóa gồm:

```text
ORDER_DISCOUNT
PRODUCT_DISCOUNT
CATEGORY_DISCOUNT
SELLER_DISCOUNT
PRODUCT_LIST_DISCOUNT
```

Không cộng nhiều Voucher cùng nhóm lên cùng eligible amount trừ khi sau này có rule P2 rõ ràng.

### 6.7. Discount Breakdown

Checkout phải trả rõ:

```text
items_subtotal
product_discount
order_discount
shipping_fee
shipping_discount
final_total
```

Server là nguồn authoritative.

### 6.8. Contract cho Checkout

Người 2 cung cấp cho Người 3 các contract logic:

```text
VoucherCandidate
VoucherValidationResult
VoucherReservationResult
VoucherDiscountBreakdown
```

Các hành động tối thiểu:

```text
validate
reserve
consume
release
```

Người 3 không tự viết lại logic tính Voucher.

## 7. Integration
Nhận Người 1:
```text
ProductSummary
PurchasableProduct
```

Cho Người 3:
```text
CheckoutCartItem[]
AddressSnapshot
VoucherCandidate[]
VoucherValidationResult
VoucherReservationResult
VoucherDiscountBreakdown
```

Nhận/giao tiếp với Người 3:
```text
CheckoutSessionCreated
OrderCreated
OrderCancelled
CheckoutExpired
```

Mục đích:
```text
reserve Voucher
consume Voucher
release Voucher
```

## 8. Mock
Cung cấp CartItem và Address fixture cho Người 3.

## 9. Test
Identity:
- duplicate email.
- wrong password.
- refresh.
- role escalation.
- address ownership.

Cart:
- add/update/remove.
- quantity 0.
- duplicate item.
- cart ownership.
- fake price ignored.
- inactive Product rejected.

Promotion/Voucher:
- expired/not started.
- inactive.
- minimum order/subtotal.
- global usage limit.
- per-customer usage limit.
- wrong Product.
- wrong Category.
- wrong Seller.
- Product list eligibility.
- freeship only affects shipping fee.
- percentage max discount.
- reserve duplicate.
- consume duplicate.
- release duplicate.
- expired reservation release.
- stacking conflict.
- final total never negative.

## 10. Deliverable
- Auth/Profile/Address UI.
- Cart UI.
- Identity Service.
- Cart Service.
- JWT/RBAC.
- Cart API.
- Promotion/Voucher Engine.
- Voucher Admin/Seller UI.
- Voucher Wallet/Apply UI.
- Reservation lifecycle.
- Stacking rules.
- Swagger.
- Unit/Integration Test.
- Fixtures.
- Demo.
- Tài liệu domain.
