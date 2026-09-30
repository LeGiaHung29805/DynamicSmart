# Catalog & Inventory API — bàn giao P0

Tài liệu này mô tả contract do `catalog-service` cung cấp cho frontend, Cart và Order/Checkout. Mọi URL phía trình duyệt đi qua API Gateway; service chạy mặc định tại cổng `8082` khi phát triển local.

## Quy ước chung

- Response thành công dùng envelope `{ "data": ... }`.
- Response lỗi dùng `code`, `message`, `errors`, `traceId` và HTTP status phù hợp.
- ID là UUID; tiền là số nguyên VND; thời gian là ISO-8601 UTC.
- Endpoint quản trị cần JWT có role `ADMIN`.
- Endpoint `/internal/**` không dành cho browser, bắt buộc `X-Internal-Api-Key`.
- Thao tác tồn kho có tác động lặp lại bắt buộc UUID tại header `Idempotency-Key`.

## Cấu trúc `catalog_db`

Flyway tạo schema P0 trong `V1__initial_schema.sql` và bổ sung index/publication field trong `V2__catalog_publication_and_indexes.sql`.

| Nhóm | Bảng |
|---|---|
| Danh mục và thuộc tính | `categories`, `attributes`, `attribute_options`, `category_attributes` |
| Sản phẩm và biến thể | `products`, `product_variants`, `product_images`, `product_attribute_values`, `variant_attribute_values` |
| Tồn kho | `inventory_items`, `inventory_reservations`, `inventory_reservation_items`, `inventory_operation_log`, `inventory_adjustments` |
| Hạ tầng tin cậy | `outbox_events`, `processed_events`, `idempotency_records` |

Các ràng buộc chính gồm SKU/slug/code duy nhất; giá và tồn không âm; `reserved_qty <= on_hand_qty`; ảnh Variant phải thuộc đúng Product; mỗi Product/Variant chỉ có một ảnh chính; mỗi Checkout Session chỉ có một Inventory Reservation. Không có khóa ngoại sang database của service khác. Thiết kế cột và index đầy đủ nằm trong `docs/09_DATABASE_DESIGN.md`.

## API công khai cho Catalog UI

| Method | Endpoint | Mục đích |
|---|---|---|
| `GET` | `/api/v1/catalog/categories` | Cây danh mục đang hoạt động |
| `GET` | `/api/v1/catalog/categories/{slug}` | Chi tiết danh mục |
| `GET` | `/api/v1/catalog/categories/{slug}/filters` | Schema bộ lọc động theo thuộc tính |
| `GET` | `/api/v1/catalog/products` | Tìm kiếm/lọc/sắp xếp/phân trang phía server |
| `GET` | `/api/v1/catalog/products/{slug}` | ProductDetail cùng Variant, ảnh, giá và tồn |
| `GET` | `/api/v1/catalog/variants/{variantId}` | Dữ liệu mới nhất của một Variant |

Query của danh sách sản phẩm:

- `keyword`: tên, slug, mô tả ngắn, SKU hoặc tên Variant.
- `categoryId`: gồm cả danh mục con đang hoạt động.
- `minimumPriceVnd`, `maximumPriceVnd`.
- `featured=true|false`.
- `attribute=CODE:value`; có thể lặp tối đa 20 lần.
- `sort=NEWEST|PRICE_ASC|PRICE_DESC|BEST_SELLER`.
- `page` bắt đầu từ `0`, `size` từ `1` đến `100`.

Chỉ Product `ACTIVE`, đã phát hành và có ít nhất một Variant `ACTIVE` còn tồn khả dụng được trả trong danh sách. `BEST_SELLER`, direct sale, voucher eligibility và rating có sẵn vị trí trong DTO nhưng cần nguồn dữ liệu thật từ owner Reporting/Cart/Engagement.

## API quản trị

### Danh mục và thuộc tính

- `/api/v1/catalog/admin/categories`: list, get, create, update và patch trạng thái.
- `/api/v1/catalog/admin/attributes`: list, get, create, update và patch trạng thái.
- `/api/v1/catalog/admin/attributes/{attributeId}/options`: tạo/sửa/đổi trạng thái lựa chọn.
- `/api/v1/catalog/admin/categories/{categoryId}/attributes`: list, upsert hoặc gỡ ánh xạ thuộc tính của danh mục.

Product/Variant chỉ nhận giá trị của Attribute đã ánh xạ đúng `PRODUCT` hoặc `VARIANT`; thuộc tính bắt buộc, kiểu dữ liệu, option và validation config được kiểm tra tại server.

### Product, Variant và ảnh

- `/api/v1/catalog/admin/products`: list phân trang, create và get.
- `/api/v1/catalog/admin/products/{productId}`: update.
- `/api/v1/catalog/admin/products/{productId}/status`: chuyển `DRAFT`, `ACTIVE`, `INACTIVE`, `ARCHIVED`.
- `/api/v1/catalog/admin/products/{productId}/variants`: tạo Variant.
- `/api/v1/catalog/admin/products/{productId}/variants/{variantId}`: sửa Variant.
- `/api/v1/catalog/admin/products/{productId}/variants/{variantId}/status`: đổi trạng thái Variant.
- `/api/v1/catalog/admin/products/{productId}/images`: thêm ảnh.
- `/api/v1/catalog/admin/products/{productId}/images/{imageId}`: sửa hoặc xóa ảnh.

Ảnh hỗ trợ JPEG, PNG, WEBP, AVIF và tối đa 5 MB. Ảnh Variant phải thuộc đúng Product; mỗi Product hoặc Variant chỉ có một ảnh chính. Product/Variant không có endpoint xóa cứng.

### Điều chỉnh tồn kho

- `GET /api/v1/catalog/admin/inventory?page=0&size=20`.
- `POST /api/v1/catalog/admin/inventory/{variantId}/adjustments` với header `Idempotency-Key` và body `{ "quantityDelta": 10, "reason": "Nhập kho" }`.
- `GET /api/v1/catalog/admin/inventory/{variantId}/adjustments`.

Server khóa dòng tồn kho khi điều chỉnh, không cho `onHandQuantity` nhỏ hơn `reservedQuantity`, đồng thời lưu tồn trước/sau, lý do, admin và operation key.

## Contract nội bộ cho Cart và Order/Checkout

### Xác thực Variant

`POST /api/v1/catalog/internal/variants/validate`

```json
{
  "items": [
    { "variantId": "uuid", "quantity": 2 }
  ]
}
```

Response trả Product/Variant/SKU, giá niêm yết do Catalog sở hữu, cân nặng/kích thước, tồn khả dụng và cờ `purchasable`. Service gọi phải dùng dữ liệu response mới nhất, không tin giá/tồn từ browser.

### Giữ, chốt và trả tồn

| Method | Endpoint | Body chính |
|---|---|---|
| `POST` | `/api/v1/catalog/internal/inventory/reservations` | `checkoutSessionId`, `expiresAt`, `items[]` |
| `GET` | `/api/v1/catalog/internal/inventory/reservations/{reservationId}` | — |
| `POST` | `/api/v1/catalog/internal/inventory/reservations/{reservationId}/commit` | `orderId` |
| `POST` | `/api/v1/catalog/internal/inventory/reservations/{reservationId}/release` | `reason` |

Reserve/commit/release khóa theo operation và dòng tồn kho, lưu request hash cùng response để retry cùng key không tạo tác động lần hai. Retry cùng key nhưng payload khác bị từ chối. Scheduler tự trả reservation quá hạn.

## Event Outbox

Catalog ghi business change và outbox trong cùng transaction, sau đó publisher gửi qua binding `catalogEvents-out-0` đến `catalog.events`:

- `InventoryReserved`
- `InventoryCommitted`
- `InventoryReleased`
- `InventoryReservationExpired`
- `InventoryAdjusted`

Envelope gồm `eventId`, `aggregateType`, `aggregateId`, `eventType`, `eventVersion`, `payload`, `correlationId`, `occurredAt`. Consumer phải xử lý at-least-once theo `eventId`.

## Chạy local và kiểm thử

1. Sao chép biến môi trường từ `services/catalog-service/.env.example` vào Run Configuration hoặc file `.env` không commit.
2. Tạo `catalog_db`, chạy PostgreSQL và RabbitMQ.
3. Profile `local` nạp dữ liệu demo bằng `db/seed/local/R__seed_catalog_demo.sql` sau Flyway.
4. Chạy test tại `services/catalog-service` bằng Maven; bộ hiện tại kiểm tra query động, trạng thái public, rule thuộc tính, oversell, khóa/điều chỉnh và idempotency reservation.

## Điểm nối thuộc owner khác

- Gateway đã whitelist `GET /api/v1/catalog/categories/**`, `GET /api/v1/catalog/products/**` và `GET /api/v1/catalog/variants/**`; endpoint admin vẫn cần JWT `ADMIN`.
- Cart owner cung cấp API thêm giỏ và direct-sale/voucher; frontend Catalog hiện chỉ gửi `variantId`, `quantity`.
- Order/Checkout owner cung cấp Buy Now session và giữ selection bằng cookie ký TTL 15 phút; frontend không gửi giá/tồn.
- Reporting owner cung cấp thứ tự bán chạy chỉ từ Order `COMPLETED`.
- Engagement owner cung cấp rating đã duyệt.
- Identity owner cung cấp forgot/reset password; frontend đã hoàn tất form và contract gọi API.
