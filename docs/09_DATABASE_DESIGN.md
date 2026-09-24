# DYNAMICMART — THIẾT KẾ CƠ SỞ DỮ LIỆU

> Tài liệu thiết kế logic cho PostgreSQL. Đây là nguồn để các owner viết Flyway migration; không phải khẳng định rằng schema đã tồn tại trong source hiện tại.

## 1. Quy ước chung

DynamicMart là B2C một doanh nghiệp bán cho nhiều customer. Có 6 logical database, mỗi service chỉ ghi/đọc database do mình sở hữu.

| Database | Owner | Mục đích |
|---|---|---|
| `identity_db` | Identity Service | tài khoản, phiên đăng nhập, địa chỉ |
| `catalog_db` | Catalog Service | catalog, variant, tồn kho, reservation |
| `cart_db` | Cart Service | cart, voucher, voucher reservation |
| `order_db` | Order Service | checkout session, Order, snapshot, lifecycle |
| `payment_db` | Payment Service | payment, VNPay/COD, cache danh mục GHN, GHN quote |
| `engagement_db` | Engagement Service | wishlist, review, Q&A, notification, chat P2, reporting |

Quy ước bắt buộc:

- Public/logical ID: `uuid`, do application sinh; không lộ sequence ID.
- Tiền Việt Nam: `bigint` đơn vị VND, không dùng `float`/`double`. Ví dụ `199000` nghĩa là 199.000đ.
- Thời gian: `timestamptz`, lưu UTC. Bảng mutable có `created_at`, `updated_at`; bảng append-only/immutable có tối thiểu timestamp tạo hoặc xảy ra sự kiện như `created_at`, `quoted_at`, `processed_at`.
- Tên bảng/cột: `snake_case`; khóa chính là `id`; khóa ngoại trong cùng database dùng `<entity>_id`.
- Chỉ tạo foreign key trong cùng logical database. ID service khác chỉ là logical reference, **không** tạo cross-database FK.
- `status`, `role`, `payment_method` lưu `varchar` và được kiểm tra bằng application + database `CHECK` khi migration; không dùng PostgreSQL enum để dễ mở rộng/versioning.
- Dữ liệu snapshot của Order không bị sửa theo Product, Address, Voucher hoặc GHN quote về sau.
- Không log token, password hay raw secret. Payload callback thanh toán chỉ lưu phần cần audit và phải redaction dữ liệu nhạy cảm.

### 1.1. Ràng buộc toàn vẹn bắt buộc khi viết migration

- Mọi cột `status`, `role`, `scope`, `payment_timing`, `payment_method`, `owner_type`, `source`, `actor_type` và `operation_type` có tập giá trị đã liệt kê trong tài liệu phải là `NOT NULL` (trừ khi bảng ghi rõ `null`) và có `CHECK` đúng tập giá trị đó.
- Mọi amount/discount/fee/subtotal/total lưu VND phải là `NOT NULL` kèm SQL `CHECK (column >= 0)` khi bản ghi đã ở trạng thái có giá trị authoritative. Với Checkout draft có thể chưa Preview, các tổng tiền có thể `null`; trước khi `Create Order` phải được materialize và kiểm tra.
- Với Checkout đã Preview và mọi Order: `items_subtotal_vnd = items_list_subtotal_vnd - direct_sale_discount_vnd`; `final_total_vnd = items_subtotal_vnd - product_discount_vnd - order_discount_vnd + shipping_fee_vnd - shipping_discount_vnd`. Mỗi discount không được vượt phần tiền tương ứng và tổng không được âm. Tổng discount phân bổ ở `order_items` phải bằng discount header tương ứng.
- FK nội bộ mặc định `ON DELETE RESTRICT`; dữ liệu lịch sử dùng deactivate/archive thay vì xóa. Các rule không biểu diễn được bằng FK/CHECK (state transition, cycle Category, lock quota/tồn kho) phải có transaction test hoặc trigger được nêu rõ bên dưới.

### 1.2. Bảng kỹ thuật dùng chung theo từng database

Mỗi service tạo các bảng kỹ thuật này trong **database của chính service** nếu service có command idempotent hoặc publish/consume event.

#### `outbox_events`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | `eventId` duy nhất, dùng cho deduplication toàn hệ thống. |
| `aggregate_type` | `varchar(80)` | Loại aggregate phát event, ví dụ `ORDER`, `PAYMENT`. |
| `aggregate_id` | `uuid` | Logical ID của aggregate. |
| `event_type` | `varchar(120)` | Ví dụ `OrderCompleted`, `PaymentSucceeded`. |
| `event_version` | `int` | Version schema của payload. |
| `payload` | `jsonb` | Nội dung event đã được serialize. |
| `correlation_id` | `uuid` | Nối toàn bộ một checkout/saga. |
| `status` | `varchar(20)` | `PENDING`, `PUBLISHED`, `FAILED`. |
| `attempt_count` | `int` | Số lần publish retry. |
| `available_at` | `timestamptz` | Thời điểm worker được retry. |
| `published_at` | `timestamptz null` | Thời điểm RabbitMQ nhận thành công. |
| `created_at` | `timestamptz` | Thời điểm insert, cùng transaction với business update. |

Index: `(status, available_at)`. Không update business state sau khi publish; worker chỉ đọc `PENDING` rồi publish/retry.

#### `processed_events`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `event_id` | `uuid PK` | Chặn cùng event được xử lý hai lần. |
| `event_type` | `varchar(120)` | Hỗ trợ audit/debug consumer. |
| `producer` | `varchar(80)` | Service phát event. |
| `processed_at` | `timestamptz` | Thời điểm consumer xử lý xong. |
| `correlation_id` | `uuid null` | Tra cứu saga. |

Consumer insert `event_id` trong cùng transaction với side effect. Nếu trùng khóa chính thì ACK/no-op.

#### `idempotency_records`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `idempotency_key` | `uuid` | Key từ client hoặc service gọi nội bộ. |
| `operation` | `varchar(100)` | Phân biệt `CREATE_ORDER`, `RESERVE_INVENTORY`, `RELEASE_VOUCHER`... |
| `request_hash` | `char(64)` | Phát hiện cùng key nhưng payload khác. |
| `status_code` | `int` | HTTP/business result lần xử lý đầu. |
| `response_body` | `jsonb` | Kết quả trả lại cho retry. |
| `expires_at` | `timestamptz` | Thời điểm có thể dọn bản ghi theo policy. |
| `created_at` | `timestamptz` | Audit. |

Khóa chính tổng hợp: `(operation, idempotency_key)`.

### 1.3. Bản đồ database và bảng theo service

Bảng dưới là mục lục schema đích để cả nhóm thấy nhanh ownership. Nó **không đồng nghĩa mọi migration đã được triển khai**; mỗi owner phải tạo Flyway migration trong service của mình. Chi tiết cột, khóa, index và quy tắc nghiệp vụ nằm ở các mục tiếp theo.

| Database | Service sở hữu | Bảng nghiệp vụ chính |
|---|---|---|
| `identity_db` | Identity Service | `users`, `refresh_tokens`, `password_reset_tokens`, `user_management_audits`, `addresses` |
| `catalog_db` | Catalog Service | `categories`, `attributes`, `attribute_options`, `category_attributes`, `products`, `product_variants`, `product_images`, `product_attribute_values`, `variant_attribute_values`, `inventory_items`, `inventory_reservations`, `inventory_reservation_items`, `inventory_operation_log`, `inventory_adjustments` |
| `cart_db` | Cart & Promotion Service | `carts`, `cart_items`, `direct_price_promotions`, `direct_price_promotion_variants`, `vouchers`, `voucher_products`, `voucher_categories`, `customer_vouchers`, `voucher_reservations` |
| `order_db` | Checkout & Order Service | `checkout_sessions`, `checkout_session_items`, `checkout_session_vouchers`, `checkout_shipping_quotes`, `orders`, `order_addresses`, `order_items`, `order_voucher_snapshots`, `order_shipping_snapshots`, `order_status_history`, `order_operation_log` |
| `payment_db` | Payment Service | `payments`, `payment_attempts`, `payment_callback_audits`, `ghn_location_provinces`, `ghn_location_wards` |
| `engagement_db` | Engagement Service | `wishlists`, `wishlist_items`, `reviews`, `review_images`, `product_questions`, `product_answers`, `notifications`, `chat_conversations`, `chat_messages`, `daily_sales_metrics`, `daily_product_metrics` |

Mỗi database bổ sung `outbox_events`, `processed_events` và `idempotency_records` khi service tương ứng phát event, nhận event hoặc có command cần chống lặp. Không tạo một database kỹ thuật dùng chung và không tạo khóa ngoại giữa các database.

Riêng `catalog_db` đã có schema P0 trong `services/catalog-service/src/main/resources/db/migration/V1__initial_schema.sql`; `V2__catalog_publication_and_indexes.sql` bổ sung thời điểm công khai, cờ nổi bật và index phục vụ search/filter. Mọi thay đổi tiếp theo phải tạo migration mới, không sửa file đã áp dụng ở môi trường dùng chung.

---

## 2. `identity_db` — Identity Service

### 2.1. `users`

Tài khoản xác thực của customer và admin. Không có seller/store.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | User ID được các service khác tham chiếu logic. |
| `email` | `varchar(254) NOT NULL` | Email khách nhập. |
| `email_normalized` | `varchar(254) UNIQUE NOT NULL` | Email lowercase/trim để unique không phân biệt hoa thường. |
| `password_hash` | `varchar(255) NOT NULL` | Hash BCrypt/Argon2, không lưu password thô. |
| `full_name` | `varchar(150) NOT NULL` | Tên hiển thị/account holder. |
| `phone` | `varchar(20) null` | Số liên hệ profile; số nhận hàng snapshot nằm ở Address/Order. |
| `role` | `varchar(20) NOT NULL` | Chỉ `CUSTOMER` hoặc `ADMIN`. Register public chỉ sinh `CUSTOMER`. |
| `status` | `varchar(20) NOT NULL` | `ACTIVE`, `LOCKED`, `DISABLED`; chặn login/checkout khi không active. |
| `auth_version` | `int NOT NULL DEFAULT 0` | Tăng khi admin đổi role/status để JWT/session cũ bị vô hiệu hóa theo version. |
| `email_verified_at` | `timestamptz null` | Dấu mốc xác minh email nếu triển khai. |
| `last_login_at` | `timestamptz null` | Audit bảo mật. |
| `created_at`, `updated_at` | `timestamptz` | Audit lifecycle. |

Index: unique `email_normalized`; index `(role, status)` cho admin listing. Admin list/search thêm index `email_normalized`, `phone` và full-text/trigram index `full_name` khi PostgreSQL extension được chốt.

### 2.2. `refresh_tokens`

Quản lý refresh token dạng hash và rotation; access JWT không lưu DB.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Token record ID. |
| `user_id` | `uuid FK -> users.id` | Chủ sở hữu token. |
| `token_hash` | `char(64) UNIQUE NOT NULL` | Hash token, không lưu token gốc. |
| `family_id` | `uuid NOT NULL` | Nhóm token rotation; phát hiện reuse. |
| `expires_at` | `timestamptz NOT NULL` | Hết hạn refresh token. |
| `revoked_at` | `timestamptz null` | Logout/revoke thời điểm nào. |
| `replaced_by_id` | `uuid FK -> refresh_tokens.id null` | Link token mới sau refresh rotation. |
| `created_at` | `timestamptz` | Audit session. |

Index: `(user_id, expires_at)`; dọn các token hết hạn/revoked theo retention policy.

### 2.3. `password_reset_tokens`

Token đặt lại mật khẩu do module xác thực nền chung tạo. Không lưu token gốc hoặc tiết lộ email có tồn tại qua API quên mật khẩu.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Reset request ID. |
| `user_id` | `uuid FK -> users.id` | User được phép đổi mật khẩu. |
| `token_hash` | `char(64) UNIQUE NOT NULL` | Hash token một lần; token thô chỉ xuất hiện trong kênh gửi an toàn. |
| `expires_at` | `timestamptz NOT NULL` | Token hết hạn theo cấu hình bảo mật. |
| `used_at` | `timestamptz null` | Token chỉ dùng một lần; có giá trị thì không reset lại được. |
| `requested_ip_hash` | `char(64) null` | Dấu vết chống lạm dụng, không lưu IP thô nếu không cần. |
| `created_at` | `timestamptz NOT NULL` | Audit thời điểm yêu cầu. |

Index `(user_id, expires_at)`. Khi reset thành công, đánh dấu token `used_at`, revoke toàn bộ Refresh Token của user và tăng `auth_version` trong cùng transaction. Request quên mật khẩu cho email không tồn tại vẫn trả response chung nhưng không tạo token.

### 2.4. `user_management_audits`

Lịch sử bất biến của mọi thao tác quản lý tài khoản do admin thực hiện. Không lưu password, token hoặc dữ liệu bí mật.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Audit ID. |
| `target_user_id` | `uuid FK -> users.id` | User bị tác động. |
| `actor_admin_id` | `uuid FK -> users.id` | Admin thực hiện thao tác. |
| `action` | `varchar(30) NOT NULL` | `LOCK`, `UNLOCK`, `DISABLE`, `CHANGE_ROLE`. |
| `old_role`, `new_role` | `varchar(20) null` | Role trước/sau nếu đổi role. |
| `old_status`, `new_status` | `varchar(20) null` | Status trước/sau nếu đổi status. |
| `reason` | `varchar(500) NOT NULL` | Lý do bắt buộc để vận hành/audit. |
| `idempotency_key` | `uuid NOT NULL` | Chống ghi audit/action lặp do retry. |
| `created_at` | `timestamptz` | Thời điểm thao tác. |

Ràng buộc unique `(actor_admin_id, idempotency_key)`. Bảng chỉ insert, không update/delete từ application.

### 2.5. `addresses`

Sổ địa chỉ của customer. Một địa chỉ được Order Service copy thành snapshot, nên sửa Address sau này không đổi đơn cũ.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Address ID. |
| `user_id` | `uuid FK -> users.id` | Customer sở hữu địa chỉ. |
| `recipient_name` | `varchar(150) NOT NULL` | Người nhận hàng. |
| `phone` | `varchar(20) NOT NULL` | Số liên hệ giao hàng. |
| `address_line` | `varchar(500) NOT NULL` | Số nhà, đường, ghi chú địa chỉ chi tiết. |
| `province_id` | `int NOT NULL` | Mã Tỉnh/Thành phố canonical từ catalog GHN. |
| `ward_id` | `int NOT NULL` | Mã Phường/Xã canonical từ catalog GHN; phải thuộc `province_id`. |
| `province_name`, `ward_name` | `varchar(150) NOT NULL` | Nhãn canonical để hiển thị; không dùng thay mã khi quote/validate. |
| `location_validated_at` | `timestamptz NOT NULL` | Thời điểm Payment Service xác nhận cặp địa giới hợp lệ. |
| `is_default` | `boolean NOT NULL DEFAULT false` | Địa chỉ mặc định trên UI. |
| `status` | `varchar(20) NOT NULL DEFAULT 'ACTIVE'` | `ACTIVE` được chọn Checkout; `INACTIVE` là ngừng dùng, không xóa lịch sử. |
| `deactivated_at` | `timestamptz null` | Audit thời điểm customer ngừng dùng địa chỉ. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

Ràng buộc: partial unique index một `is_default = true` cho mỗi `user_id` **và `status = ACTIVE`**. API `DELETE` là deactivate (`INACTIVE`), không physical delete. Identity chỉ insert/update sau khi gọi Location Validation của Payment Service. Customer chỉ được chọn address `ACTIVE` của chính mình khi checkout. Customer tự nhập `address_line` nhưng phải chọn Tỉnh/Thành phố → Phường/Xã từ catalog GHN; không có cột quận/huyện trong mô hình hiện hành.

---

## 3. `catalog_db` — Catalog & Inventory Service

### 3.1. `categories`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Category ID. |
| `parent_id` | `uuid FK -> categories.id null` | Cây category; `null` là root. |
| `code` | `varchar(80) UNIQUE NOT NULL` | Mã ổn định cho integration/import. |
| `name` | `varchar(200) NOT NULL` | Tên hiển thị. |
| `slug` | `varchar(220) UNIQUE NOT NULL` | URL SEO-friendly. |
| `description` | `text null` | Mô tả category. |
| `status` | `varchar(20)` | `ACTIVE`, `INACTIVE`. |
| `sort_order` | `int NOT NULL DEFAULT 0` | Thứ tự hiển thị. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

Service phải chặn vòng lặp cây Category (một Category không được là ancestor của chính nó) trong cùng transaction tạo/sửa `parent_id`; migration có thể dùng trigger recursive để bảo vệ cả import/SQL trực tiếp.

### 3.2. `attributes`

Định nghĩa thuộc tính động dùng lại giữa category, ví dụ RAM, màu, size.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Attribute ID. |
| `code` | `varchar(80) UNIQUE NOT NULL` | Key ổn định, ví dụ `color`, `ram`. |
| `name` | `varchar(120) NOT NULL` | Nhãn hiển thị. |
| `data_type` | `varchar(20)` | `TEXT`, `NUMBER`, `DECIMAL`, `BOOLEAN`, `SELECT`, `MULTI_SELECT`. |
| `validation_config` | `jsonb null` | min/max, regex, unit hoặc rule riêng. |
| `status` | `varchar(20)` | `ACTIVE`, `INACTIVE`. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

### 3.3. `attribute_options`

Option cho attribute loại `SELECT`/`MULTI_SELECT`.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Option ID. |
| `attribute_id` | `uuid FK -> attributes.id` | Attribute chủ quản. |
| `code` | `varchar(80) NOT NULL` | Giá trị máy đọc, unique trong một attribute. |
| `label` | `varchar(120) NOT NULL` | Nhãn UI. |
| `sort_order` | `int` | Thứ tự hiển thị. |
| `status` | `varchar(20)` | `ACTIVE`, `INACTIVE`. |

Ràng buộc unique: `(attribute_id, code)`.

### 3.4. `category_attributes`

Mapping category–attribute để form và filter không bị hard-code theo Fashion.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Mapping ID. |
| `category_id` | `uuid FK -> categories.id` | Category dùng thuộc tính. |
| `attribute_id` | `uuid FK -> attributes.id` | Thuộc tính được gắn. |
| `applies_to` | `varchar(20)` | `PRODUCT` hoặc `VARIANT`. |
| `is_required` | `boolean` | Bắt buộc khi admin tạo/sửa entity. |
| `is_filterable` | `boolean` | Cho phép xuất hiện ở search/filter. |
| `sort_order` | `int` | Thứ tự form/UI. |

Ràng buộc unique: `(category_id, attribute_id, applies_to)`.

### 3.5. `products`

Container thông tin chung; không phải saleable unit.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Product logical ID. |
| `category_id` | `uuid FK -> categories.id` | Category hiện hành. |
| `name` | `varchar(300) NOT NULL` | Tên sản phẩm. |
| `slug` | `varchar(330) UNIQUE NOT NULL` | URL product. |
| `short_description` | `varchar(1000) null` | Tóm tắt listing. |
| `description` | `text null` | Mô tả chi tiết. |
| `status` | `varchar(20)` | `DRAFT`, `ACTIVE`, `INACTIVE`, `ARCHIVED`. |
| `default_weight_grams` | `int null` | Fallback cân nặng nếu variant không override. |
| `default_length_cm`, `default_width_cm`, `default_height_cm` | `int null` | Kích thước gói hàng mặc định cho GHN quote. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

### 3.6. `product_variants`

Đơn vị duy nhất có thể bán, đặt vào Cart, reserve tồn và xuất hiện trong OrderItem.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | `variantId` dùng xuyên service. |
| `product_id` | `uuid FK -> products.id` | Product cha. |
| `sku` | `varchar(100) UNIQUE NOT NULL` | SKU quản trị/kho. |
| `name` | `varchar(300) null` | Nhãn variant, ví dụ “Đen / 128GB”. |
| `price_vnd` | `bigint NOT NULL CHECK >= 0` | Giá niêm yết authoritative của Variant, trước giảm giá trực tiếp/voucher. |
| `weight_grams` | `int NOT NULL CHECK > 0` | Cân nặng bắt buộc cho GHN quote. |
| `length_cm`, `width_cm`, `height_cm` | `int null CHECK > 0` | Override kích thước product default nếu cần. |
| `status` | `varchar(20)` | `ACTIVE`, `INACTIVE`, `ARCHIVED`. Variant inactive không checkout. |
| `sort_order` | `int` | Thứ tự selector. |
| `version` | `bigint NOT NULL DEFAULT 0` | Optimistic locking khi admin sửa. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

Index: `(product_id, status)`. Catalog validation chặn variant `ACTIVE` khi thiếu shipping dimension sau khi áp fallback product.

### 3.7. `product_images`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Image ID. |
| `product_id` | `uuid FK -> products.id` | Product sở hữu ảnh. |
| `variant_id` | `uuid FK -> product_variants.id null` | Nếu có giá trị, ảnh chỉ cho variant này. |
| `image_url` | `varchar(1000) NOT NULL` | URL ảnh trên object storage/CDN. |
| `alt_text` | `varchar(300) null` | Accessibility/SEO. |
| `sort_order` | `int` | Thứ tự gallery. |
| `is_primary` | `boolean NOT NULL DEFAULT false` | Ảnh chính của product/variant. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

Để ngăn một ảnh gắn Product A nhưng Variant của Product B, tạo unique key `(id, product_id)` trên `product_variants` và composite FK `(variant_id, product_id) -> product_variants(id, product_id)` khi `variant_id` không null. Tạo partial unique index một primary ảnh Product (`product_id` với `variant_id is null`) và một primary ảnh cho mỗi Variant (`variant_id is not null`).

### 3.8. `product_attribute_values` và `variant_attribute_values`

Hai bảng có cấu trúc tương tự để bảo toàn FK và tách attribute cấp Product/Variant.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Value ID. |
| `product_id` / `variant_id` | `uuid FK` | Entity được gán giá trị. |
| `attribute_id` | `uuid FK -> attributes.id` | Định nghĩa type/rule của giá trị. |
| `value_json` | `jsonb NOT NULL` | Giá trị typed; ví dụ string, number, boolean hoặc array option code. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

Ràng buộc unique: `(product_id, attribute_id)` hoặc `(variant_id, attribute_id)`. Application validate `value_json` theo `data_type`, option và `category_attributes` trước khi lưu.

### 3.9. `inventory_items`

Số tồn hiện tại theo variant; không lưu stock trong Product.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `variant_id` | `uuid PK FK -> product_variants.id` | Một inventory row cho một variant. |
| `on_hand_qty` | `int NOT NULL CHECK >= 0` | Hàng thực có trong kho. |
| `reserved_qty` | `int NOT NULL DEFAULT 0 CHECK >= 0` | Tổng hàng đang bị Saga tạo Order hoặc chờ thanh toán trả trước giữ. |
| `version` | `bigint NOT NULL DEFAULT 0` | Lock/version chống oversell. |
| `created_at`, `updated_at` | `timestamptz` | Audit/lần điều chỉnh gần nhất. |

Tồn có thể bán = `on_hand_qty - reserved_qty`. Reserve dùng transaction/row lock để không bao giờ âm.

### 3.10. `inventory_reservations`

Header một lần giữ tồn do Saga Create Order tạo; không được tạo chỉ vì customer mở Checkout Preview.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | `reservationId` trả cho Order Service. |
| `checkout_session_id` | `uuid NOT NULL` | Logical ID từ `order_db`, không FK cross DB. |
| `order_id` | `uuid null` | Logical ID được điền sau khi Order tạo thành công. |
| `status` | `varchar(20)` | `RESERVED`, `COMMITTED`, `RELEASED`, `EXPIRED`. |
| `expires_at` | `timestamptz NOT NULL` | Với prepaid, không vượt TTL Payment; postpaid/FREE được commit ngay. |
| `committed_at`, `released_at` | `timestamptz null` | Audit transition. |
| `release_reason` | `varchar(80) null` | `CREATE_ORDER_FAILED`, `PAYMENT_FAILED`, `PAYMENT_EXPIRED`, ... |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

Index: `(status, expires_at)` cho expiry worker; unique `(checkout_session_id)` cho một reservation header/session.

### 3.11. `inventory_reservation_items`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Detail ID. |
| `reservation_id` | `uuid FK -> inventory_reservations.id` | Header reservation. |
| `variant_id` | `uuid FK -> product_variants.id` | Variant được giữ hàng. |
| `quantity` | `int NOT NULL CHECK > 0` | Số lượng giữ/commit/release. |

Ràng buộc unique `(reservation_id, variant_id)`; commit/release chỉ thực hiện đúng một lần theo status reservation.

### 3.12. `inventory_operation_log`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `operation_key` | `uuid PK` | Idempotency key của reserve/commit/release. |
| `operation_type` | `varchar(30)` | `RESERVE`, `COMMIT`, `RELEASE`. |
| `reservation_id` | `uuid null` | Reservation bị thao tác. |
| `result_payload` | `jsonb` | Kết quả để trả lại khi retry. |
| `created_at` | `timestamptz` | Audit. |

### 3.13. `inventory_adjustments`

Audit append-only cho chức năng admin điều chỉnh tồn; không dùng `inventory_operation_log` vì bảng đó chỉ idempotency cho reserve/commit/release của Saga.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Adjustment ID. |
| `operation_key` | `uuid UNIQUE NOT NULL` | Idempotency key của command admin. |
| `variant_id` | `uuid FK -> product_variants.id` | Variant bị điều chỉnh. |
| `quantity_delta` | `int NOT NULL CHECK <> 0` | Lượng tăng/giảm on-hand. |
| `on_hand_before`, `on_hand_after` | `int NOT NULL CHECK >= 0` | Số tồn trước/sau để audit. |
| `reason` | `varchar(500) NOT NULL` | Lý do bắt buộc. |
| `actor_admin_id` | `uuid NOT NULL` | Logical Identity ID của admin thực hiện. |
| `created_at` | `timestamptz NOT NULL` | Audit thời điểm điều chỉnh. |

Command lock row `inventory_items`, kiểm tra `on_hand_after >= reserved_qty`, cập nhật `on_hand_qty`/`version` và insert `inventory_adjustments` trong cùng transaction. `COMMIT` reservation vẫn có thể truy ra số lượng qua reservation items; bảng này chỉ lưu thao tác điều chỉnh thủ công.

---

## 4. `cart_db` — Cart & Promotion Service

### 4.1. `carts`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Cart ID. |
| `owner_type` | `varchar(20)` | `CUSTOMER` hoặc `GUEST` cho Guest Cart P1. |
| `customer_id` | `uuid null` | Logical user ID khi cart thuộc customer. |
| `guest_token_hash` | `char(64) null` | Hash anonymous cookie, không lưu token thô. |
| `status` | `varchar(20)` | `ACTIVE`, `MERGED`, `ABANDONED`. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

`CHECK` bảo đảm `CUSTOMER` có `customer_id`, `GUEST` có `guest_token_hash`. Partial unique index một active cart/customer và một active cart/guest token.

### 4.2. `cart_items`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Cart item ID. |
| `cart_id` | `uuid FK -> carts.id` | Cart chứa dòng hàng. |
| `variant_id` | `uuid NOT NULL` | Logical Catalog variant ID. |
| `quantity` | `int NOT NULL CHECK > 0` | Số lượng mong muốn. |
| `version` | `bigint NOT NULL DEFAULT 0` | Tăng mỗi lần sửa; bảo vệ dọn Cart sau Order không xóa thay đổi mới. |
| `is_selected` | `boolean NOT NULL DEFAULT true` | Dòng được đưa vào checkout. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

Unique `(cart_id, variant_id)` để một variant chỉ có một CartItem. Không lưu/không tin giá ở cart; Checkout luôn gọi Catalog.

### 4.3. `direct_price_promotions`

Chiến dịch **giảm giá trực tiếp** của sản phẩm. Nó khác Voucher: không cần customer nhập/chọn mã, giá sale hiển thị được ngay trên Product Card/Detail khi campaign đang hiệu lực.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Direct sale ID. |
| `name`, `description` | `varchar(200)`, `text null` | Tên/nội dung campaign cho Admin và UI. |
| `status` | `varchar(20)` | `DRAFT`, `ACTIVE`, `PAUSED`, `EXPIRED`, `ARCHIVED`. |
| `discount_method` | `varchar(20)` | `FIXED_AMOUNT` hoặc `PERCENTAGE`. |
| `fixed_discount_vnd` | `bigint null CHECK >= 0` | Số tiền giảm cho một đơn vị Variant. |
| `discount_rate_bps` | `int null CHECK > 0` | Phần trăm giảm; `1500 = 15%`. |
| `max_discount_vnd` | `bigint null CHECK >= 0` | Mức giảm tối đa nếu theo phần trăm. |
| `starts_at`, `ends_at` | `timestamptz NOT NULL` | Thời gian sale; `ends_at > starts_at`. |
| `created_by` | `uuid NOT NULL` | Logical admin ID tạo campaign. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

`CHECK` bảo đảm chỉ một trong `fixed_discount_vnd`/`discount_rate_bps` phù hợp method. P0 chỉ cho một direct sale có hiệu lực trên một Variant tại cùng thời điểm; service chặn target window trùng nhau. Giá sale server tính từ `product_variants.price_vnd`, không ghi đè giá niêm yết Catalog.

### 4.4. `direct_price_promotion_variants`

Target Variant của direct sale, dùng logical `variant_id` vì Catalog sở hữu `catalog_db`.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `promotion_id` | `uuid FK -> direct_price_promotions.id` | Campaign cha. |
| `variant_id` | `uuid NOT NULL` | Logical Catalog Variant được sale. |
| `created_at` | `timestamptz` | Audit. |

Unique `(promotion_id, variant_id)`. Admin có thể chọn nhiều Variant trong cùng campaign; giá sale được áp dụng độc lập theo giá niêm yết từng Variant.

### 4.5. `vouchers`

Định nghĩa rule voucher. Voucher mặc định có `is_default = true`, nhưng vẫn chỉ được trả qua API sau JWT và server filter eligibility.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Voucher ID. |
| `code` | `varchar(80) UNIQUE NOT NULL` | Mã chuẩn hóa uppercase/trim. |
| `name` | `varchar(200) NOT NULL` | Tên voucher hiển thị. |
| `description` | `text null` | Điều kiện marketing. |
| `status` | `varchar(20)` | `DRAFT`, `ACTIVE`, `DISABLED`, `EXPIRED`. |
| `scope` | `varchar(30)` | `ORDER_DISCOUNT`, `SHIPPING_DISCOUNT`, `PRODUCT_DISCOUNT`, `CATEGORY_DISCOUNT`, `PRODUCT_LIST_DISCOUNT`. |
| `discount_method` | `varchar(20)` | `FIXED_AMOUNT` hoặc `PERCENTAGE`. |
| `fixed_discount_vnd` | `bigint null` | Giá trị giảm cố định; chỉ dùng cho `FIXED_AMOUNT`. |
| `discount_rate_bps` | `int null` | Phần trăm theo basis point; `1000` = 10%; chỉ dùng cho `PERCENTAGE`. |
| `max_discount_vnd` | `bigint null` | Trần tiền giảm cho percentage. |
| `minimum_order_vnd` | `bigint null` | Tổng đơn tối thiểu. |
| `minimum_eligible_subtotal_vnd` | `bigint null` | Tổng hàng đủ điều kiện tối thiểu. |
| `usage_limit` | `int null` | Tổng lượt consume tối đa; null = không giới hạn. |
| `consumed_count` | `int NOT NULL DEFAULT 0` | Bộ đếm atomically tăng khi consume. |
| `usage_limit_per_customer` | `int null` | Số lần một customer được consume. |
| `starts_at`, `ends_at` | `timestamptz` | Khoảng hiệu lực. |
| `distribution_mode` | `varchar(30) NOT NULL` | `DEFAULT_FOR_ELIGIBLE`, `ASSIGNED_ONLY`, `CODE_ONLY`; quyết định ai nhìn/thêm được voucher. |
| `is_default` | `boolean NOT NULL DEFAULT false` | Chỉ hợp lệ với `DEFAULT_FOR_ELIGIBLE`; voucher mặc định hiển thị cho customer đã đăng nhập và đủ điều kiện sơ bộ. |
| `created_by` | `uuid` | Logical admin user ID tạo/sửa. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

`CHECK` bảo đảm chỉ một trong `fixed_discount_vnd`/`discount_rate_bps` phù hợp method. Không cho discount làm `final_total` âm ở Voucher Engine.

### 4.6. `voucher_products` và `voucher_categories`

Các bảng scope target, dùng logical ID vì Product/Category nằm ở `catalog_db`.

| Bảng | Cột chính | Tác dụng |
|---|---|---|
| `voucher_products` | `voucher_id FK`, `product_id uuid`, `created_at` | Target Product cho `PRODUCT_DISCOUNT`/`PRODUCT_LIST_DISCOUNT`. |
| `voucher_categories` | `voucher_id FK`, `category_id uuid`, `created_at` | Target Category cho `CATEGORY_DISCOUNT`. |

Unique lần lượt `(voucher_id, product_id)` và `(voucher_id, category_id)`.

### 4.7. `customer_vouchers`

Wallet/assignment riêng cho customer. Voucher global mặc định không cần tạo row cho mọi customer.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Assignment ID. |
| `customer_id` | `uuid NOT NULL` | Logical Identity user ID. |
| `voucher_id` | `uuid FK -> vouchers.id` | Voucher được cấp. |
| `status` | `varchar(20)` | `AVAILABLE`, `USED_UP`, `REVOKED`, `EXPIRED`. |
| `assigned_at` | `timestamptz` | Thời điểm cấp. |
| `expires_at` | `timestamptz null` | Expiry riêng, không vượt rule voucher. |
| `source` | `varchar(30)` | `ADMIN_ASSIGNMENT`, `CAMPAIGN`, `WALLET`. |
| `assigned_by` | `uuid null` | Logical Admin ID cấp thủ công; null khi hệ thống/campaign tự cấp. |

Unique `(customer_id, voucher_id)` cho scope P0; đồng thời tạo unique `(id, customer_id, voucher_id)` để `voucher_reservations` có thể FK đúng assignment. Mở rộng campaign nhiều bản ghi sau này cần thiết kế lại.

### 4.8. `voucher_reservations`

Giữ quota voucher trong CheckoutSession để hai customer không consume vượt giới hạn.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | `voucherReservationId`. |
| `voucher_id` | `uuid FK -> vouchers.id` | Voucher được giữ. |
| `customer_id` | `uuid NOT NULL` | Customer đang dùng. |
| `checkout_session_id` | `uuid NOT NULL` | Logical session ID từ `order_db`. |
| `customer_voucher_id` | `uuid null` | FK tới voucher trong Wallet; null chỉ với voucher global/default. |
| `order_id` | `uuid null` | Logical Order ID khi consume. |
| `scope` | `varchar(30)` | Snapshot scope tại lúc reserve. |
| `status` | `varchar(20)` | `RESERVED`, `CONSUMED`, `RELEASED`, `EXPIRED`. |
| `discount_amount_vnd` | `bigint NOT NULL` | Số tiền giảm do server tính tại session. |
| `shipping_discount_vnd` | `bigint NOT NULL DEFAULT 0` | Phần giảm phí ship. |
| `reserved_until` | `timestamptz NOT NULL` | `min(payment.expires_at, voucher.ends_at)` khi prepaid; postpaid/FREE consume ngay khi OrderConfirmed. |
| `consumed_at`, `released_at` | `timestamptz null` | Audit transition. |
| `release_reason` | `varchar(80) null` | `CREATE_ORDER_FAILED`, `PAYMENT_FAILED` hoặc `PAYMENT_EXPIRED`. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

`customer_voucher_id`, khi có giá trị, phải khớp đúng `(customer_id, voucher_id)` qua composite FK `(customer_voucher_id, customer_id, voucher_id) -> customer_vouchers(id, customer_id, voucher_id)`; nhờ đó expiry/status `USED_UP`/`REVOKED` của Wallet không bị bỏ qua. Index `(status, reserved_until)`. Unique `(checkout_session_id, voucher_id)`; Voucher Engine dùng lock/counter transaction để enforce total/per-customer usage.

Quy tắc: chỉ `RESERVED` mới release. Voucher đã `CONSUMED` của Order xác nhận không trả quota nếu giao vận thất bại do ngoại lệ vận hành.

---

## 5. `order_db` — Checkout & Order Service

### 5.1. `checkout_sessions`

Aggregate trước khi có Order. Session chỉ thuộc customer đã đăng nhập và hết hạn sau 15 phút.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | `checkoutSessionId`, correlation cho reservation. |
| `customer_id` | `uuid NOT NULL` | Logical Identity user ID. |
| `source` | `varchar(20) NOT NULL` | `CART` hoặc `BUY_NOW`; xác định nguồn item của session. |
| `cart_id` | `uuid null` | Logical Cart ID nguồn; bắt buộc khi `source = CART`, phải null khi `BUY_NOW`. |
| `selection_fingerprint` | `char(64) NOT NULL` | Hash server-side của source, item/quantity và customer; phát hiện session/request khác dữ liệu. |
| `address_id` | `uuid null` | Logical address ID được chọn; có thể null khi session vừa mở, nhưng bắt buộc trước Preview/Create Order. Dữ liệu chi tiết copy vào quote/order snapshot. |
| `status` | `varchar(20)` | `ACTIVE`, `COMPLETED`, `CANCELLED`, `EXPIRED`. |
| `payment_timing` | `varchar(20)` | Lựa chọn `PREPAID`, `POSTPAID`, `NOT_REQUIRED`. |
| `payment_method` | `varchar(20)` | `VNPAY`, `COD`, `FREE`; backend validate cặp hợp lệ. |
| `items_list_subtotal_vnd` | `bigint` | Tổng giá niêm yết Variant trước direct sale. |
| `direct_sale_discount_vnd` | `bigint` | Tổng giảm từ direct sale đang hiệu lực. |
| `items_subtotal_vnd` | `bigint` | Tổng item sau direct sale, trước Voucher. |
| `product_discount_vnd` | `bigint` | Giảm theo Product/Category/List. |
| `order_discount_vnd` | `bigint` | Giảm theo Order voucher. |
| `shipping_fee_vnd` | `bigint` | GHN fee được quote. |
| `shipping_discount_vnd` | `bigint` | Freeship/shipping voucher. |
| `final_total_vnd` | `bigint` | Tổng server authoritative, `>= 0`. |
| `expires_at` | `timestamptz NOT NULL` | `created_at + 15 phút`. |
| `completed_order_id` | `uuid null` | Logical Order ID sau confirm; dùng chặn create lại. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

Index `(customer_id, status, expires_at)` và `(status, expires_at)` cho expiry job. `CHECK` bắt buộc `source = CART` có `cart_id`, `source = BUY_NOW` không có `cart_id`; service/trigger transaction bảo đảm session Buy Now có đúng một `checkout_session_items` row. `address_id` có thể null khi customer đang thêm địa chỉ, nhưng Preview/Create Order phải từ chối nếu null. Khi `final_total_vnd = 0`, backend ép `NOT_REQUIRED + FREE`.

### 5.2. `checkout_session_items`

Snapshot item dùng để reserve và tạo Order, tránh Cart bị thay đổi giữa preview/confirm.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Detail ID. |
| `checkout_session_id` | `uuid FK -> checkout_sessions.id` | Session cha. |
| `source_cart_item_id` | `uuid null` | CartItem gốc khi source là `CART`; null với `BUY_NOW`. |
| `source_cart_item_version` | `bigint null` | Version CartItem lúc snapshot; dùng dọn Cart an toàn sau OrderConfirmed. |
| `product_id`, `variant_id` | `uuid NOT NULL` | Logical Catalog IDs. |
| `sku` | `varchar(100)` | SKU tại thời điểm snapshot. |
| `product_name`, `variant_name` | `varchar(300)` | Tên lịch sử. |
| `image_url` | `varchar(1000) null` | Ảnh lịch sử. |
| `list_price_vnd` | `bigint NOT NULL` | Giá niêm yết Variant lúc checkout. |
| `direct_sale_promotion_id` | `uuid null` | Logical direct sale đã áp dụng; null nếu không sale. |
| `direct_sale_discount_vnd` | `bigint NOT NULL DEFAULT 0` | Giảm trực tiếp trên một đơn vị. |
| `unit_price_vnd` | `bigint NOT NULL` | Giá sau direct sale, trước Voucher, tại Checkout. |
| `quantity` | `int NOT NULL CHECK > 0` | Số lượng. |
| `weight_grams` | `int NOT NULL` | Input GHN quote snapshot. |
| `created_at` | `timestamptz` | Audit. |

Unique `(checkout_session_id, variant_id)`.

### 5.3. `checkout_session_vouchers`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Detail ID. |
| `checkout_session_id` | `uuid FK -> checkout_sessions.id` | Session cha. |
| `voucher_id` | `uuid NOT NULL` | Logical Cart voucher ID. |
| `voucher_reservation_id` | `uuid null` | Chỉ được điền sau khi Saga reserve thành công; Preview không tạo reservation. |
| `voucher_code` | `varchar(80)` | Code snapshot hiển thị. |
| `scope` | `varchar(30)` | Scope snapshot. |
| `discount_amount_vnd` | `bigint NOT NULL` | Số giảm áp dụng. |
| `created_at` | `timestamptz` | Audit. |

Đây là bảng **selection/snapshot Preview**. Customer đổi voucher thì cập nhật hoặc xóa row; Session/Preview không reserve quota. Khi `Create Order`, Saga reserve Voucher trước rồi mới gắn `voucher_reservation_id`; Order snapshot dùng dữ liệu sau lần validate cuối. P0 tối đa một voucher hàng hóa và một voucher shipping trong một session; enforce bằng hai partial unique index trên `checkout_session_id`: `WHERE scope = 'SHIPPING_DISCOUNT'` và `WHERE scope <> 'SHIPPING_DISCOUNT'`.

### 5.4. `checkout_shipping_quotes`

Lưu quote server-side để biết chính xác tổng tiền được dùng khi confirm.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Quote snapshot ID. |
| `checkout_session_id` | `uuid FK -> checkout_sessions.id` | Session được quote. |
| `provider` | `varchar(20)` | `GHN` ở P0. |
| `service_id` | `varchar(50)` | GHN service đã chọn. |
| `fee_vnd` | `bigint NOT NULL CHECK >= 0` | Phí ship trả về từ GHN. |
| `eta_text` | `varchar(200) null` | ETA trả về để hiển thị. |
| `total_weight_grams` | `int NOT NULL CHECK > 0` | Input cần audit/requote. |
| `package_length_cm`, `package_width_cm`, `package_height_cm` | `int null CHECK > 0` | Kích thước package rule server. |
| `input_fingerprint` | `char(64) NOT NULL` | Hash authoritative của customer, address canonical, item/quantity/weight, shipping voucher, service và kho gửi. |
| `status` | `varchar(20) NOT NULL` | `ACTIVE`, `CONSUMED`, `INVALIDATED`, `EXPIRED`; chỉ `ACTIVE` và đúng fingerprint được dùng Create Order. |
| `consumed_at` | `timestamptz null` | Chặn quote được dùng cho nhiều Order. |
| `to_province_id`, `to_ward_id` | `int`, `int` | Điểm đến Tỉnh/Thành phố và Phường/Xã tại lúc quote. |
| `to_province_name`, `to_ward_name` | `varchar(150)` | Nhãn canonical phục vụ audit/history quote. |
| `quoted_at` | `timestamptz NOT NULL` | Thời điểm nhận quote. |
| `expires_at` | `timestamptz NOT NULL` | Không quá expiry session. |
| `raw_response_redacted` | `jsonb null` | Dữ liệu cần audit, không chứa credential. |

Khi quote GHN timeout/invalid address, không tạo row hợp lệ và không cho Create Order bằng fee frontend/fallback. Create Order tính lại `input_fingerprint`; address/item/quantity/weight/voucher/service/kho gửi đổi, quote hết hạn hoặc status khác `ACTIVE` đều bị từ chối và buộc quote lại.

### 5.5. `orders`

Aggregate đơn hàng và state machine. Customer không có API hủy sau khi row này được tạo.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | `orderId` dùng xuyên service. |
| `order_number` | `varchar(32) UNIQUE NOT NULL` | Mã đơn human-readable. |
| `checkout_session_id` | `uuid UNIQUE NOT NULL` | Một session chỉ tạo một Order. |
| `customer_id` | `uuid NOT NULL` | Logical user ID phục vụ ownership. |
| `status` | `varchar(40)` | `PENDING_PAYMENT`, `CONFIRMED`, `PACKING`, `SHIPPING`, `HANDOVER_PENDING`, `DELIVERED`, `COMPLETED`, `CANCELLED`. |
| `payment_timing` | `varchar(20)` | Snapshot `PREPAID`, `POSTPAID`, `NOT_REQUIRED`. |
| `payment_method` | `varchar(20)` | Snapshot `VNPAY`, `COD`, `FREE`. |
| `items_list_subtotal_vnd` | `bigint NOT NULL CHECK >= 0` | Tổng giá niêm yết trước direct sale. |
| `direct_sale_discount_vnd` | `bigint NOT NULL CHECK >= 0` | Tổng giảm trực tiếp snapshot. |
| `items_subtotal_vnd` | `bigint NOT NULL CHECK >= 0` | Tổng item sau direct sale, trước Voucher. |
| `product_discount_vnd` | `bigint NOT NULL CHECK >= 0` | Giảm item scope. |
| `order_discount_vnd` | `bigint NOT NULL CHECK >= 0` | Giảm order scope. |
| `shipping_fee_vnd` | `bigint NOT NULL CHECK >= 0` | Phí GHN snapshot. |
| `shipping_discount_vnd` | `bigint NOT NULL CHECK >= 0` | Giảm phí ship snapshot. |
| `final_total_vnd` | `bigint NOT NULL CHECK >= 0` | Số tiền Payment/Reporting dùng. |
| `currency` | `char(3) NOT NULL DEFAULT 'VND'` | Explicit currency cho lịch sử. |
| `payment_due_at` | `timestamptz null` | Thời điểm bắt đầu yêu cầu payment trả sau tại bàn giao. |
| `payment_succeeded_at` | `timestamptz null` | Flag event PaymentSucceeded đã xử lý. |
| `shipment_delivered_at` | `timestamptz null` | Thời điểm customer xác nhận đã nhận hàng và Order phát `ShipmentDelivered`. |
| `cancel_reason` | `varchar(80) null` | P0 chỉ `PREPAID_PAYMENT_FAILED` hoặc `PREPAID_PAYMENT_EXPIRED`. |
| `cancelled_at`, `confirmed_at`, `completed_at` | `timestamptz null` | Mốc state quan trọng. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

P0 không có Shipment aggregate riêng. `DELIVERED` là customer đã xác nhận nhận hàng; admin không được set trạng thái này. Với mọi Order, `COMPLETED` chỉ khi `shipment_delivered_at` có giá trị và điều kiện thanh toán đạt: Payment `PAID` với prepaid/postpaid, hoặc `NOT_REQUIRED + FREE`. Với postpaid, `PaymentSucceeded` và `ShipmentDelivered` đến trước/sau đều an toàn; Order dừng ở `HANDOVER_PENDING` hoặc `DELIVERED` cho đến khi đủ cả hai. Nếu giao và thanh toán cùng đã đủ, ghi `DELIVERED` trong `order_status_history` rồi chuyển `COMPLETED` trong cùng transaction.

`CANCELLED` ở P0 chỉ cho `PENDING_PAYMENT` của `PREPAID + VNPAY` khi Payment `FAILED`/`EXPIRED`; không có hủy Order thủ công hoặc cancellation vận hành sau `CONFIRMED` trong scope P0. Nếu mở scope này sau, phải thiết kế riêng transaction hoàn tồn đã commit và policy hoàn tiền.

### 5.6. `order_addresses`

Snapshot địa chỉ giao hàng; không FK tới `identity_db.addresses`.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Snapshot ID. |
| `order_id` | `uuid UNIQUE FK -> orders.id` | Một Order có một delivery address P0. |
| `source_address_id` | `uuid null` | Logical Address gốc, chỉ để trace. |
| `recipient_name`, `phone`, `address_line` | `varchar` | Bản sao dữ liệu giao hàng. |
| `province_id`, `ward_id` | `int`, `int` | Mapping GHN hai cấp tại lúc Order. |
| `province_name`, `ward_name` | `varchar` | Nhãn canonical hiển thị lịch sử. |
| `created_at` | `timestamptz` | Audit. |

### 5.7. `order_items`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | `orderItemId`, dùng kiểm tra review eligibility. |
| `order_id` | `uuid FK -> orders.id` | Order cha. |
| `product_id`, `variant_id` | `uuid NOT NULL` | Logical Catalog references để navigation/report. |
| `sku` | `varchar(100)` | SKU lịch sử. |
| `product_name`, `variant_name` | `varchar(300)` | Tên lịch sử. |
| `image_url` | `varchar(1000) null` | Ảnh lịch sử. |
| `list_price_vnd` | `bigint NOT NULL` | Giá niêm yết trước direct sale. |
| `direct_sale_promotion_id` | `uuid null` | Logical direct sale đã áp dụng lúc tạo Order. |
| `direct_sale_discount_vnd` | `bigint NOT NULL DEFAULT 0` | Giảm trực tiếp phân bổ trên dòng. |
| `unit_price_vnd` | `bigint NOT NULL` | Giá sau direct sale, trước Voucher. |
| `quantity` | `int NOT NULL CHECK > 0` | Số lượng. |
| `product_discount_vnd` | `bigint NOT NULL DEFAULT 0` | Giảm phân bổ từ product/category/list voucher. |
| `order_discount_vnd` | `bigint NOT NULL DEFAULT 0` | Giảm order phân bổ xuống item để reporting. |
| `line_total_vnd` | `bigint NOT NULL` | Tổng dòng sau discount, trước shipping. |
| `weight_grams` | `int NOT NULL` | Snapshot logistics/reporting. |
| `created_at` | `timestamptz` | Audit. |

Index `(order_id)` và `(product_id)`; values immutable sau create.

### 5.8. `order_voucher_snapshots`

Lịch sử voucher độc lập với Cart voucher có thể bị sửa/tắt.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Snapshot ID. |
| `order_id` | `uuid FK -> orders.id` | Order cha. |
| `voucher_id` | `uuid null` | Logical source voucher ID. |
| `voucher_code`, `scope`, `discount_method` | `varchar` | Rule hiển thị lịch sử. |
| `discount_value` | `bigint null` | Fixed value hoặc value presentation at time applied. |
| `eligible_subtotal_vnd` | `bigint` | Cơ sở tính discount. |
| `discount_amount_vnd` | `bigint` | Giảm hàng. |
| `shipping_discount_vnd` | `bigint` | Giảm ship. |
| `created_at` | `timestamptz` | Audit. |

### 5.9. `order_shipping_snapshots`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Shipping snapshot ID. |
| `order_id` | `uuid UNIQUE FK -> orders.id` | Order cha. |
| `provider` | `varchar(20)` | `GHN`. |
| `service_id` | `varchar(50)` | Service quote được chọn. |
| `source_checkout_quote_id` | `uuid NOT NULL FK -> checkout_shipping_quotes.id` | Quote đã consume; trace về quote server-side. |
| `input_fingerprint` | `char(64) NOT NULL` | Fingerprint authoritative của quote đã dùng. |
| `fee_vnd` | `bigint NOT NULL CHECK >= 0` | Fee trước voucher shipping. |
| `eta_text` | `varchar(200) null` | ETA tại thời điểm tạo Order. |
| `from_province_id`, `from_ward_id` | `int` | Kho gửi cố định snapshot theo catalog GHN. |
| `to_province_id`, `to_ward_id` | `int` | Destination snapshot theo catalog GHN. |
| `total_weight_grams` | `int NOT NULL CHECK > 0` | Input quote. |
| `package_length_cm`, `package_width_cm`, `package_height_cm` | `int null CHECK > 0` | Kích thước package đã gửi GHN. |
| `quoted_at` | `timestamptz NOT NULL` | Mốc quote. |

Không có `tracking_code` hay `ghn_order_code` trong scope hiện tại vì GHN chỉ quote fee.

### 5.10. `order_status_history`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | History ID. |
| `order_id` | `uuid FK -> orders.id` | Order thay đổi. |
| `from_status`, `to_status` | `varchar(40)` | Kiểm toán state transition. |
| `actor_type` | `varchar(20)` | `SYSTEM`, `ADMIN`, `CUSTOMER`, `PAYMENT_EVENT`. |
| `actor_id` | `uuid null` | Logical admin/customer ID nếu thao tác thủ công. |
| `reason` | `varchar(500) null` | Lý do/ghi chú thao tác. |
| `correlation_id`, `source_event_id` | `uuid null` | Trace saga và dedupe. |
| `created_at` | `timestamptz` | Thời điểm đổi trạng thái. |

### 5.11. `order_operation_log`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `operation_key` | `uuid PK` | Idempotency key của Create/Confirm/Cancel session hoặc admin command. |
| `operation_type` | `varchar(50)` | Ví dụ `CREATE_ORDER`, `CANCEL_CHECKOUT_SESSION`, `CONFIRM_RECEIVED`. |
| `order_id` | `uuid null` | Order liên quan. |
| `result_payload` | `jsonb` | Response cho retry. |
| `created_at` | `timestamptz` | Audit. |

---

## 6. `payment_db` — Payment, VNPay & GHN Quote

### 6.1. `payments`

Chỉ tạo cho Order có `final_total_vnd > 0`; đơn `NOT_REQUIRED + FREE` không có row Payment.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | `paymentId`. |
| `order_id` | `uuid UNIQUE NOT NULL` | Logical Order ID, một payment aggregate/Order P0. |
| `customer_id` | `uuid NOT NULL` | Logical payer. |
| `payment_timing` | `varchar(20)` | `PREPAID` hoặc `POSTPAID`. |
| `payment_method` | `varchar(20)` | `VNPAY` hoặc `COD`. |
| `amount_vnd` | `bigint NOT NULL CHECK > 0` | Server amount snapshot phải khớp Order. |
| `currency` | `char(3) DEFAULT 'VND'` | Currency explicit. |
| `status` | `varchar(20)` | Prepaid: `PENDING`, `PAID`, `FAILED`, `EXPIRED`; postpaid: chỉ `PENDING`, `PAID`. |
| `paid_at` | `timestamptz null` | Khi hệ thống xác thực đã thu tiền. |
| `expires_at` | `timestamptz null` | TTL prepaid; postpaid QR TTL ở attempt. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

`PREPAID + VNPAY` tạo Payment/Attempt URL ngay sau Order. `POSTPAID` tạo Payment `PENDING` lúc Order; chỉ `POSTPAID + VNPAY` phát `PaymentDue` khi Order vào `HANDOVER_PENDING`, Payment Service nhận event này để mở VNPay Attempt. VNPay URL/QR trả sau `FAILED`/`EXPIRED` chỉ làm Attempt kết thúc, Payment vẫn `PENDING` để có thể tạo Attempt mới. COD không dùng `PaymentDue` và không tạo Attempt thành công cho đến khi admin xác nhận thu tiền tại bàn giao.

### 6.2. `payment_attempts`

Mỗi URL VNPay/retry hoặc lần ghi nhận COD là một attempt; không ghi đè reference cũ.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Attempt ID. |
| `payment_id` | `uuid FK -> payments.id` | Payment aggregate. |
| `attempt_no` | `int NOT NULL` | Số lần thử tăng dần. |
| `channel` | `varchar(20)` | `VNPAY` hoặc `COD`. |
| `provider_reference` | `varchar(100) UNIQUE null` | Mã đơn/reference ký gửi VNPay. |
| `expected_amount_vnd` | `bigint NOT NULL` | Amount phải nhận. |
| `status` | `varchar(20)` | `CREATED`, `PENDING`, `SUCCEEDED`, `FAILED`, `EXPIRED`. |
| `payment_url` | `text null` | URL redirect; không log vào event/public log. |
| `expires_at` | `timestamptz null` | TTL 15 phút cho VNPay URL. |
| `provider_transaction_id` | `varchar(120) UNIQUE null` | Transaction ID VNPay khi success. |
| `received_amount_vnd` | `bigint null` | Amount callback/COD collection. |
| `provider_response_code` | `varchar(50) null` | Mã kết quả provider. |
| `collection_reference` | `varchar(120) null` | Biên nhận/mã đối soát khi thu COD. |
| `recorded_by_admin_id` | `uuid null` | Logical Identity ID của admin xác nhận COD; null với VNPay IPN. |
| `recorded_at` | `timestamptz null` | Mốc admin ghi nhận thu COD. |
| `requested_at`, `resolved_at` | `timestamptz` | Audit lifecycle. |

Unique `(payment_id, attempt_no)`. Với attempt `COD` thành công, bắt buộc `received_amount_vnd = expected_amount_vnd`, `recorded_by_admin_id`, `recorded_at` và `collection_reference`; trạng thái không được đổi trực tiếp qua UI. Postpaid VNPay QR hết hạn tạo attempt/reference mới khi Order còn `HANDOVER_PENDING`; không hủy Order.

### 6.3. `payment_callback_audits`

Lưu mọi VNPay IPN/return, kể cả invalid/duplicate/late, nhưng không làm side effect nếu không hợp lệ.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Audit ID. |
| `payment_attempt_id` | `uuid FK -> payment_attempts.id null` | Attempt match được, nếu có. |
| `provider` | `varchar(20)` | `VNPAY`. |
| `provider_reference` | `varchar(100)` | Reference nhận được. |
| `provider_transaction_id` | `varchar(120) null` | Transaction provider gửi. |
| `checksum_valid` | `boolean` | Kết quả xác thực signature. |
| `amount_valid` | `boolean` | So với expected amount. |
| `status_before`, `status_after` | `varchar(20) null` | Audit state effect. |
| `decision` | `varchar(30)` | `ACCEPTED`, `REJECTED`, `DUPLICATE`, `LATE_AUDIT`. |
| `payload_redacted` | `jsonb` | Payload đã bỏ checksum/PII không cần thiết. |
| `received_at` | `timestamptz` | Khi IPN tới. |

Customer confirm-received thuộc **Order Service**, không tạo bảng giao hàng riêng trong `payment_db`. Order Service ghi `shipment_delivered_at`, insert `order_status_history` với actor `CUSTOMER` và publish `ShipmentDelivered` qua Outbox. Payment Service chỉ xử lý trạng thái Payment/QR/COD.

### 6.4. `ghn_location_provinces`

Cache danh mục Tỉnh/Thành phố do GHN cung cấp. Payment Service là owner; Identity/Order chỉ gọi API/contract, không sao chép bảng này sang database khác.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `province_id` | `int PK` | Mã Tỉnh/Thành phố canonical từ GHN. |
| `name` | `varchar(150) NOT NULL` | Tên hiển thị canonical. |
| `name_extensions` | `jsonb null` | Các alias/tên mở rộng do provider trả về, hỗ trợ tìm kiếm UI. |
| `status` | `varchar(20) NOT NULL` | `ACTIVE` hoặc `INACTIVE`; chỉ `ACTIVE` được trả ra form Address. |
| `provider_updated_at` | `timestamptz null` | Mốc dữ liệu provider nếu có. |
| `synced_at` | `timestamptz NOT NULL` | Thời điểm cache được đồng bộ. |

Index `(status, name)` cho endpoint list/search. Không nhận province do frontend tự tạo.

### 6.5. `ghn_location_wards`

Cache Phường/Xã phụ thuộc một Tỉnh/Thành phố trong catalog GHN hai cấp.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `ward_id` | `int PK` | Mã Phường/Xã canonical từ GHN. |
| `province_id` | `int NOT NULL FK -> ghn_location_provinces.province_id` | Đảm bảo ward thuộc đúng province. |
| `name` | `varchar(150) NOT NULL` | Tên canonical để render/snapshot. |
| `name_extensions` | `jsonb null` | Alias/tên mở rộng phục vụ tìm kiếm UI. |
| `status` | `varchar(20) NOT NULL` | `ACTIVE` hoặc `INACTIVE`; chỉ active được chọn. |
| `provider_updated_at` | `timestamptz null` | Mốc provider nếu có. |
| `synced_at` | `timestamptz NOT NULL` | Thời điểm đồng bộ cache. |

Index `(province_id, status, name)`. `POST /internal/v1/locations/validate` kiểm tra cặp `(province_id, ward_id, status = ACTIVE)` rồi trả các tên canonical. Khi refresh danh mục lỗi, không nhận text/mã địa giới từ client làm fallback.

---

## 7. `engagement_db` — Wishlist, Review, Q&A, Notification & Reporting

### 7.1. `wishlists`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Wishlist ID. |
| `customer_id` | `uuid UNIQUE NOT NULL` | Logical user ID; một wishlist/customer P0. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

### 7.2. `wishlist_items`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Wishlist item ID. |
| `wishlist_id` | `uuid FK -> wishlists.id` | Wishlist cha. |
| `product_id` | `uuid NOT NULL` | Logical Catalog product ID. |
| `created_at` | `timestamptz` | Thời điểm save. |

Unique `(wishlist_id, product_id)` để click nhiều lần không duplicate.

### 7.3. `reviews`

Review chỉ tạo khi OrderItem thuộc customer, Order `COMPLETED` và chưa review. Eligibility kiểm tra qua Order Service trước insert.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Review ID. |
| `customer_id` | `uuid NOT NULL` | Logical reviewer ID. |
| `order_id` | `uuid NOT NULL` | Logical Order ID phục vụ audit. |
| `order_item_id` | `uuid UNIQUE NOT NULL` | Bảo đảm một review/một OrderItem. |
| `product_id`, `variant_id` | `uuid NOT NULL` | Logical Catalog reference để aggregate rating. |
| `rating` | `smallint NOT NULL CHECK BETWEEN 1 AND 5` | Số sao. |
| `comment` | `text null` | Nhận xét. |
| `status` | `varchar(20)` | `VISIBLE`, `HIDDEN`. |
| `hidden_reason` | `varchar(500) null` | Lý do moderation. |
| `created_at`, `updated_at` | `timestamptz` | Audit. |

Index `(product_id, status, created_at DESC)` cho product detail.

### 7.4. `review_images`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Image ID. |
| `review_id` | `uuid FK -> reviews.id` | Review cha. |
| `image_url` | `varchar(1000) NOT NULL` | URL object storage/CDN. |
| `sort_order` | `int` | Thứ tự gallery. |
| `created_at` | `timestamptz` | Audit. |

### 7.5. `product_questions` và `product_answers` — P1

| Bảng | Cột quan trọng | Tác dụng |
|---|---|---|
| `product_questions` | `id`, `product_id`, `customer_id`, `question`, `status`, timestamps | Câu hỏi của customer trên product. `status`: `VISIBLE`, `HIDDEN`, `ANSWERED`. |
| `product_answers` | `id`, `question_id FK`, `admin_id`, `answer`, timestamps | Câu trả lời của admin; không có seller answer. |

### 7.6. `notifications` — P1

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Notification ID. |
| `customer_id` | `uuid NOT NULL` | Recipient logical ID. |
| `type` | `varchar(60)` | Ví dụ `ORDER_COMPLETED`, `PAYMENT_DUE`. |
| `title`, `body` | `varchar/text` | Nội dung in-app notification. |
| `data` | `jsonb null` | Deep-link metadata không nhạy cảm. |
| `read_at` | `timestamptz null` | Trạng thái đã đọc. |
| `created_at` | `timestamptz` | Thời điểm nhận event. |

### 7.7. `chat_conversations` — P2

Hội thoại hỗ trợ giữa Customer và Admin. Đây là thiết kế trước để mở rộng, không phải điều kiện P0/P1 và không có seller/store.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Conversation ID. |
| `customer_id` | `uuid NOT NULL` | Logical Identity ID của customer tạo hội thoại. |
| `assigned_admin_id` | `uuid null` | Logical admin đang xử lý; null khi chưa gán. |
| `subject` | `varchar(250) null` | Chủ đề ngắn hiển thị inbox. |
| `status` | `varchar(20) NOT NULL` | `OPEN`, `ASSIGNED`, `CLOSED`. |
| `last_message_at` | `timestamptz NOT NULL` | Sắp xếp inbox theo tin mới nhất. |
| `closed_at` | `timestamptz null` | Audit thời điểm đóng. |
| `created_at`, `updated_at` | `timestamptz` | Audit lifecycle. |

Index `(customer_id, status, last_message_at DESC)` cho customer và `(assigned_admin_id, status, last_message_at DESC)` cho admin inbox. Không có FK vật lý sang Identity DB.

### 7.8. `chat_messages` — P2

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `id` | `uuid PK` | Message ID. |
| `conversation_id` | `uuid FK -> chat_conversations.id` | Hội thoại cha. |
| `sender_id` | `uuid NOT NULL` | Logical Identity ID của người gửi. |
| `sender_role` | `varchar(20) NOT NULL` | `CUSTOMER` hoặc `ADMIN`, để kiểm tra quyền và render. |
| `message_type` | `varchar(20) NOT NULL DEFAULT 'TEXT'` | P2 ban đầu chỉ `TEXT`; attachment mở rộng sau. |
| `body` | `text NOT NULL` | Nội dung đã validate/sanitize. |
| `idempotency_key` | `uuid NOT NULL` | Retry gửi message không tạo trùng. |
| `read_at` | `timestamptz null` | Mốc đã đọc nếu UI cần. |
| `created_at` | `timestamptz NOT NULL` | Thời điểm gửi/audit. |

Unique `(conversation_id, idempotency_key)`; index `(conversation_id, created_at)`. Service phải kiểm tra customer chỉ gửi/đọc conversation của mình, admin phải có role `ADMIN`.

### 7.9. `daily_sales_metrics`

Reporting chỉ consume `OrderCompleted`; không JOIN sang `order_db`.

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `metric_date` | `date` | Ngày UTC/business timezone đã chốt. |
| `completed_order_count` | `int` | Số đơn completed ngày đó. |
| `gross_item_value_vnd` | `bigint` | Tổng item trước giảm. |
| `discount_value_vnd` | `bigint` | Tổng product/order discount. |
| `shipping_fee_vnd` | `bigint` | Phí ship thực thu sau shipping discount theo policy report. |
| `completed_order_total_vnd` | `bigint` | Tổng `final_total_vnd` của OrderCompleted. |
| `updated_at` | `timestamptz` | Mốc projection update. |

Primary key: `metric_date`. P0 gọi `completed_order_total_vnd` là Revenue; không suy diễn lợi nhuận và chưa điều chỉnh refund.

### 7.10. `daily_product_metrics`

| Cột | Kiểu/ràng buộc | Tác dụng |
|---|---|---|
| `metric_date` | `date` | Ngày metric. |
| `product_id`, `variant_id` | `uuid` | Logical Catalog dimensions. |
| `category_id` | `uuid null` | Category snapshot từ OrderCompleted event nếu có. |
| `quantity_sold` | `int` | Số item của OrderCompleted. |
| `net_item_sales_vnd` | `bigint` | Tổng line total sau discount, không gồm shipping. |
| `updated_at` | `timestamptz` | Mốc projection update. |

Primary key `(metric_date, variant_id)`. Dùng để làm Best Seller P0; không query trực tiếp Catalog/Order DB khi render report.

---

## 8. Quan hệ liên database và rule snapshot

```text
identity_db.users
  └─ identity_db.addresses

catalog_db.products → product_variants → inventory_items
                                  ├─ inventory_reservations/items
                                  └─ inventory_adjustments

cart_db.carts → cart_items
cart_db.vouchers → voucher_reservations

order_db.checkout_sessions
  ├─ checkout_session_items
  ├─ checkout_session_vouchers
  ├─ checkout_shipping_quotes
  └─ orders
       ├─ order_addresses
       ├─ order_items
       ├─ order_voucher_snapshots
       ├─ order_shipping_snapshots
       └─ order_status_history

payment_db.payments → payment_attempts → payment_callback_audits
payment_db.ghn_location_provinces → ghn_location_wards

engagement_db.wishlists → wishlist_items
engagement_db.reviews → review_images
engagement_db.chat_conversations → chat_messages  (P2)
engagement_db.daily_*_metrics
```

Các logical reference quan trọng không có FK vật lý:

| Từ bảng | Đến logical ID | Lý do |
|---|---|---|
| `cart_items.variant_id` | `catalog_db.product_variants.id` | Cart không đọc DB Catalog. |
| `voucher_reservations.checkout_session_id` | `order_db.checkout_sessions.id` | Voucher reservation thuộc Cart Service. |
| `inventory_reservations.checkout_session_id` | `order_db.checkout_sessions.id` | Inventory reservation thuộc Catalog Service. |
| `orders.customer_id` | `identity_db.users.id` | Ownership kiểm tra qua JWT/contract. |
| `payments.order_id` | `order_db.orders.id` | Payment nhận `OrderPaymentContext`, không update Order DB. |
| `reviews.order_item_id` | `order_db.order_items.id` | Eligibility qua internal API/event, không cross DB join. |

---

## 9. Migration, index và dữ liệu mẫu

Mỗi service quản lý Flyway migrations trong repository của service đó:

```text
V1__create_core_tables.sql
V2__create_outbox_and_idempotency.sql
V3__add_indexes_and_constraints.sql
```

Nguyên tắc migration:

- Không sửa migration đã áp dụng ở shared environment; tạo migration mới.
- Tạo index cho mọi FK nội bộ, unique business key, status/expiry worker và query owner phổ biến.
- Tạo `CHECK` cho amount/quantity không âm, status hợp lệ và cặp payment timing/method hợp lệ khi DB kiểm tra được.
- Seed chỉ gồm `ADMIN` demo, Category/Attribute demo, ProductVariant có `weight_grams`, Voucher demo và address dùng cặp Tỉnh/Thành phố–Phường/Xã GHN hợp lệ; không seed secret VNPay/GHN.
- Mỗi migration thay đổi event payload hoặc snapshot field phải tăng `event_version`/contract version tương ứng.

## 10. Checklist trước khi code migration

- [ ] Khóa `order_number` format và timezone dùng cho `daily_*_metrics`.
- [ ] Khóa rule đóng gói: cộng/trần kích thước variant để gửi GHN.
- [ ] Khóa GHN configuration ở deployment: token, shop ID, warehouse province/ward; không đưa secret vào Git.
- [ ] Viết unique/index/locking test cho `inventory_items`, `inventory_adjustments`, `voucher_reservations`, `checkout_session_vouchers`, `orders.checkout_session_id`, `payments.order_id`, `payment_attempts.provider_reference` và ảnh Product/Variant composite FK.
- [ ] Viết constraint test cho money formula/discount allocation, cặp payment timing-method, Voucher Wallet assignment và Category cycle.
- [ ] Viết integration test cho session expiry 15 phút, prepaid payment expiry 15 phút, duplicate IPN, GHN quote failure và `final_total_vnd = 0`.
