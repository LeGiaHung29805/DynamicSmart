# NGƯỜI 1 — CATALOG & INVENTORY DOMAIN

## 1. Phạm vi
Sở hữu end-to-end:
```text
Catalog
Category
Dynamic Attribute
Product
Search / Filter
Inventory
Inventory Reservation
```

Database tổng thể không thuộc phần riêng; dùng schema/contract chung sau khi trưởng nhóm chốt.

## 2. Frontend
Customer:
- Category navigation.
- Product List.
- Product Detail.
- Search.
- Dynamic Filter.
- Sort.
- Loading/Error/Empty.

Admin/Seller:
- Category Management.
- Attribute Management.
- Attribute Option.
- Category-Attribute Mapping.
- Product Create/Edit.
- Dynamic Product Form.
- Product Image.
- Inventory.
- Low Stock.

## 3. Backend
- Catalog Service.
- Category tree.
- Dynamic Attribute schema.
- Attribute inheritance.
- Product CRUD.
- Dynamic validation.
- Search/filter.
- Product status.
- Inventory.
- Reservation.
- Commit.
- Release.
- Concurrency control.

## 4. Dynamic Attribute
Hỗ trợ:
```text
TEXT
NUMBER
DECIMAL
BOOLEAN
SELECT
MULTI_SELECT
```

Ví dụ:
```json
{
  "brand": "DELL",
  "cpu": "Intel Core i7",
  "ram": 16,
  "storage": 512,
  "storage_type": "SSD"
}
```

## 5. API
```text
GET /api/v1/categories
GET /api/v1/categories/{id}/schema
GET /api/v1/products
GET /api/v1/products/{id}

POST/PATCH /api/v1/admin/categories
POST/PATCH /api/v1/admin/attributes
POST/PATCH /api/v1/seller/products

POST /internal/v1/products/validate-checkout
POST /internal/v1/inventory/reservations
POST /internal/v1/inventory/reservations/{orderId}/commit
POST /internal/v1/inventory/reservations/{orderId}/release
```

## 6. Inventory Reservation
```text
AVAILABLE → RESERVED → COMMITTED
RESERVED → RELEASED
```

Phải:
- không oversell;
- duplicate reserve an toàn;
- duplicate commit an toàn;
- duplicate release an toàn.

## 7. Contract
Cho Người 2:
```text
ProductSummary
PurchasableProduct
```

Cho Người 3:
```text
ProductCheckoutValidation
InventoryReservationResult
```

Nhận Người 5:
```text
ReviewCreated
ReviewHidden
```

## 8. Mock
Cung cấp mock Product cho Người 2/3 ngay từ đầu.

## 9. Test
- Required attribute.
- Wrong datatype.
- Invalid option.
- Unknown attribute.
- Product inactive.
- Search/filter.
- Stock=1, hai request đồng thời.
- Duplicate reserve/commit/release.

## 10. Deliverable
- Catalog UI.
- Admin/Seller Catalog UI.
- Catalog Service.
- Dynamic Attribute Engine.
- Product APIs.
- Search/Filter.
- Inventory Reservation.
- Swagger.
- Unit/Integration Test.
- Mock fixtures.
- Demo.
- Tài liệu domain.
