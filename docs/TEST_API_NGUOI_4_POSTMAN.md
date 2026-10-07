# Hướng dẫn test API Người 4 bằng Postman

Phạm vi Người 4 gồm: danh mục địa giới, báo giá GHN, Payment/COD, VNPay, ZaloPay, PayOS, QR ngân hàng SePay và các API quản trị/audit.

## 1. Chuẩn bị

Các thành phần tối thiểu phải chạy:

- PostgreSQL tại `localhost:5432`, có database `payment_db` và `identity_db`.
- RabbitMQ tại `localhost:5672`.
- Identity Service: `http://localhost:8081`.
- Payment Service: `http://localhost:8085`.
- API Gateway: `http://localhost:8080`.

Chạy từng ứng dụng trong một terminal riêng:

```powershell
cd E:\dynamicmart\services\identity-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd E:\dynamicmart\services\payment-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd E:\dynamicmart\api-gateway
.\mvnw.cmd spring-boot:run
```

Kiểm tra nhanh:

```http
GET http://localhost:8085/actuator/health
GET http://localhost:8080/actuator/health
```

Kết quả đúng là HTTP `200` và `status = UP`.

## 2. Tạo Postman Environment

Tạo environment `DynamicMart local` với các biến:

| Biến | Giá trị ban đầu |
|---|---|
| `gateway_url` | `http://localhost:8080` |
| `payment_url` | `http://localhost:8085` |
| `internal_key` | Giá trị `INTERNAL_API_KEY` trong `.env` |
| `customer_token` | để trống |
| `admin_token` | để trống |
| `customer_id` | `00000000-0000-4000-8000-000000000002` |
| `province_id` | để trống |
| `ward_id` | để trống |
| `quote_id` | để trống |
| `quote_fingerprint` | để trống |
| `order_id` | để trống |
| `payment_id` | để trống |
| `payment_reference` | để trống |

Header chung cho API nội bộ:

```text
X-Internal-Api-Key: {{internal_key}}
Content-Type: application/json
```

Không đưa `INTERNAL_API_KEY` vào frontend hoặc commit vào Git.

## 3. Đăng nhập lấy JWT

### Customer

```http
POST {{gateway_url}}/api/v1/auth/login
Content-Type: application/json

{
  "email": "customer@dynamicmart.local",
  "password": "Password@123"
}
```

Trong tab **Tests** của Postman:

```javascript
const body = pm.response.json();
pm.environment.set("customer_token", body.accessToken);
pm.environment.set("customer_id", body.user.id);
```

### Admin

```http
POST {{gateway_url}}/api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@dynamicmart.local",
  "password": "Password@123"
}
```

Tests:

```javascript
pm.environment.set("admin_token", pm.response.json().accessToken);
```

Nếu tài khoản mẫu chưa tồn tại, hãy chạy Identity Service bằng profile `local` để Flyway nạp seed local.

## 4. Test địa giới GHN

### 4.1 Đồng bộ địa giới

```http
POST {{gateway_url}}/api/v1/locations/sync
Authorization: Bearer {{admin_token}}
X-Internal-Api-Key: {{internal_key}}
```

Kết quả đúng: HTTP `200`, ví dụ:

```json
{
  "provinceCount": 63,
  "districtCount": 700,
  "wardCount": 10000
}
```

Các con số thực tế phụ thuộc dữ liệu GHN. Nếu trả `GHN_NOT_CONFIGURED`, kiểm tra `GHN_BASE_URL`, `GHN_TOKEN`, `GHN_SHOP_ID`, `GHN_FROM_DISTRICT_ID` và `GHN_FROM_WARD_CODE`. Token production phải dùng `https://online-gateway.ghn.vn`; token staging mới dùng `https://dev-online-gateway.ghn.vn`.

### 4.2 Lấy tỉnh/thành phố

```http
GET {{gateway_url}}/api/v1/locations/provinces
```

Kết quả đúng: HTTP `200` và mảng `{ "id", "name" }`. Chọn một `id`, lưu vào `province_id`.

### 4.3 Lấy phường/xã theo tỉnh

```http
GET {{gateway_url}}/api/v1/locations/wards?provinceId={{province_id}}
```

Chọn một `id` trong response và lưu vào `ward_id`.

### 4.4 Xác thực cặp tỉnh/phường

```http
GET {{gateway_url}}/api/v1/locations/validate?provinceId={{province_id}}&wardId={{ward_id}}
X-Internal-Api-Key: {{internal_key}}
```

Kết quả đúng:

```json
{
  "valid": true,
  "provinceId": 201,
  "wardId": 11007
}
```

Test lỗi bằng cách ghép một `wardId` không thuộc tỉnh. Kết quả mong đợi là HTTP `422`, code `GHN_LOCATION_MISMATCH`.

## 5. Test báo giá GHN

### 5.1 Tạo báo giá

```http
POST {{gateway_url}}/api/v1/shipping/quotes
X-Internal-Api-Key: {{internal_key}}
Content-Type: application/json

{
  "customerId": "{{customer_id}}",
  "provinceId": {{province_id}},
  "wardId": {{ward_id}},
  "items": [
    {
      "variantId": "11111111-1111-4111-8111-111111111111",
      "quantity": 2,
      "weightGrams": 500,
      "lengthCm": 25,
      "widthCm": 20,
      "heightCm": 10
    }
  ],
  "shippingDiscountVnd": 0,
  "serviceCode": null
}
```

Tests:

```javascript
const body = pm.response.json();
pm.environment.set("quote_id", body.quoteId);
pm.environment.set("quote_fingerprint", body.requestFingerprint);
```

Kết quả đúng: HTTP `200`; có `feeVnd`, `payableFeeVnd`, `serviceId`, `eta`, `expiresAt`, `quoteId` và `requestFingerprint`.

Gửi lại đúng payload trước khi quote hết hạn phải trả lại quote còn hiệu lực, không tạo phí mới.

### 5.2 Xác thực và dùng quote

```http
POST {{gateway_url}}/api/v1/shipping/quotes/{{quote_id}}/validate
X-Internal-Api-Key: {{internal_key}}
Content-Type: application/json

{
  "customerId": "{{customer_id}}",
  "requestFingerprint": "{{quote_fingerprint}}"
}
```

Lần đầu phải HTTP `200`. Gửi lần hai phải HTTP `409`, code `SHIPPING_QUOTE_INVALID`, vì quote chỉ được dùng một lần.

## 6. Test Payment context và Payment read API

Mỗi lần test nên tạo UUID mới trong Postman:

```javascript
pm.environment.set("order_id", pm.variables.replaceIn("{{$guid}}"));
pm.environment.set("correlation_id", pm.variables.replaceIn("{{$guid}}"));
```

### 6.1 Tạo COD trả sau

```http
POST {{gateway_url}}/api/v1/payments/order-context
X-Internal-Api-Key: {{internal_key}}
Content-Type: application/json

{
  "orderId": "{{order_id}}",
  "customerId": "{{customer_id}}",
  "amountVnd": 150000,
  "timing": "POSTPAID",
  "method": "COD",
  "correlationId": "{{correlation_id}}"
}
```

Tests:

```javascript
pm.environment.set("payment_id", pm.response.json().id);
```

Kết quả đúng: HTTP `200`, `status = PENDING`, `method = COD`, `redirectUrl = null`.

Test sai bằng `timing = PREPAID`, `method = COD`; kết quả phải HTTP `422`, code `PREPAID_COD_FORBIDDEN`.

### 6.2 Lấy Payment theo ID

```http
GET {{gateway_url}}/api/v1/payments/{{payment_id}}?customerId={{customer_id}}
X-Internal-Api-Key: {{internal_key}}
Authorization: Bearer {{customer_token}}
```

### 6.3 Lấy Payment theo Order

```http
GET {{gateway_url}}/api/v1/payments/orders/{{order_id}}
X-Internal-Api-Key: {{internal_key}}
```

### 6.4 Ghi nhận COD đã thu

```http
POST {{gateway_url}}/api/v1/payments/orders/{{order_id}}/cod-collected
X-Internal-Api-Key: {{internal_key}}
Authorization: Bearer {{customer_token}}
```

Kết quả đúng: `status = PAID`, có `paidAt`, `codConfirmedBy` và `codConfirmedAt`. Gửi lại phải vẫn trả cùng Payment đã thanh toán, không tạo thu tiền lần hai.

Trong luồng thật, hai thao tác trên do service nội bộ thực hiện trực tiếp. Gateway hiện yêu cầu JWT cho các đường dẫn này ngoài `X-Internal-Api-Key`; khi chỉ muốn test contract nội bộ, có thể thay `{{gateway_url}}` bằng `{{payment_url}}` và bỏ header `Authorization`.

## 7. Test các phương thức online

Lặp request `order-context`, nhưng dùng UUID `orderId` và `correlationId` mới cho mỗi phương thức.

| Phương thức | `timing` | `method` | Kết quả mong đợi |
|---|---|---|---|
| VNPay trả trước | `PREPAID` | `VNPAY` | Có URL sandbox VNPay ngay trong `redirectUrl`. |
| ZaloPay trả trước | `PREPAID` | `ZALOPAY` | Có URL/deep link ZaloPay. |
| PayOS trả trước | `PREPAID` | `PAYOS` | Có checkout URL PayOS. |
| QR ngân hàng | `PREPAID` | `BANK_QR` | Có URL VietQR. |
| Online trả sau | `POSTPAID` | một trong bốn phương thức trên | Ban đầu chưa tạo attempt; gọi API tạo attempt khi đến bước bàn giao. |

Payload mẫu VNPay:

```json
{
  "orderId": "{{$guid}}",
  "customerId": "{{customer_id}}",
  "amountVnd": 150000,
  "timing": "PREPAID",
  "method": "VNPAY",
  "correlationId": "{{$guid}}"
}
```

Sau response, lưu:

```javascript
const body = pm.response.json();
pm.environment.set("payment_id", body.id);
```

Mở `redirectUrl` trong trình duyệt để test sandbox thật. Payment chỉ chuyển `PAID` sau callback/webhook hợp lệ, không phải chỉ vì trình duyệt quay về trang kết quả.

### 7.1 Tạo hoặc lấy lại attempt online

```http
POST {{gateway_url}}/api/v1/payments/{{payment_id}}/attempts
X-Internal-Api-Key: {{internal_key}}
```

Alias cũ của VNPay:

```http
POST {{gateway_url}}/api/v1/payments/{{payment_id}}/vnpay-attempt
X-Internal-Api-Key: {{internal_key}}
```

Nếu attempt còn hiệu lực, gọi lại phải trả URL đang dùng thay vì tạo attempt mới. COD phải trả HTTP `409`, code `PAYMENT_METHOD_NOT_ONLINE`.

### 7.2 Lấy reference từ lịch sử attempt

```http
GET {{gateway_url}}/api/v1/payments/{{payment_id}}/attempts
Authorization: Bearer {{admin_token}}
```

Lưu `content[0].reference` hoặc phần tử đầu tiên của mảng vào `payment_reference`:

```javascript
const body = pm.response.json();
const item = Array.isArray(body) ? body[0] : body.content[0];
pm.environment.set("payment_reference", item.reference);
```

### 7.3 Customer đọc trạng thái theo reference

```http
GET {{gateway_url}}/api/v1/payments/return-status?reference={{payment_reference}}
Authorization: Bearer {{customer_token}}
```

Riêng alias VNPay:

```http
GET {{gateway_url}}/api/v1/payments/vnpay/return-status?vnp_TxnRef={{payment_reference}}
Authorization: Bearer {{customer_token}}
```

Customer khác phải nhận HTTP `403`, code `PAYMENT_OWNERSHIP_DENIED`.

## 8. Test callback/webhook

### 8.1 VNPay sandbox thật

1. Tạo Payment `PREPAID + VNPAY`.
2. Mở `redirectUrl` và hoàn tất giao dịch sandbox.
3. Cấu hình VNPay IPN trỏ đến URL public của Gateway: `/api/v1/payments/vnpay/ipn`.
4. Gọi `return-status` để xác nhận `status = PAID`.
5. Xem audit bằng API admin ở mục 9.

Test chữ ký sai, không được làm Payment thành `PAID`:

```http
GET {{gateway_url}}/api/v1/payments/vnpay/ipn?vnp_TxnRef={{payment_reference}}&vnp_Amount=15000000&vnp_ResponseCode=00&vnp_TransactionStatus=00&vnp_TransactionNo=TEST-001&vnp_SecureHash=invalid
```

Kết quả đúng: `RspCode = 97`; audit có `checksumValid = false`.

### 8.2 ZaloPay callback sai chữ ký

```http
POST {{gateway_url}}/api/v1/payments/zalopay/callback
Content-Type: application/json

{
  "data": "{\"app_trans_id\":\"{{payment_reference}}\",\"amount\":150000,\"zp_trans_id\":\"TEST-ZP-001\"}",
  "mac": "invalid"
}
```

Kết quả đúng: `return_code = -1`; Payment không đổi sang `PAID`.

### 8.3 PayOS webhook sai checksum

```http
POST {{gateway_url}}/api/v1/payments/payos/webhook
Content-Type: application/json

{
  "success": true,
  "data": {
    "orderCode": "{{payment_reference}}",
    "amount": 150000,
    "reference": "TEST-PAYOS-001",
    "code": "00"
  },
  "signature": "invalid"
}
```

Kết quả đúng: `{ "success": false }`.

### 8.4 SePay webhook sai chữ ký

```http
POST {{gateway_url}}/api/v1/payments/sepay/webhook
X-SePay-Timestamp: 1700000000
X-SePay-Signature: invalid
Content-Type: application/json

{
  "id": "TEST-SEPAY-001",
  "code": "{{payment_reference}}",
  "transferAmount": 150000,
  "transferType": "in"
}
```

Kết quả đúng: `{ "success": false }`.

Callback hợp lệ nên được test bằng sandbox/provider thật để provider tạo đúng HMAC/checksum. Không tự đặt `status = PAID` trong database để thay cho callback.

## 9. Test API quản trị

### Danh sách Payment

```http
GET {{gateway_url}}/api/v1/payments?page=0&size=20
Authorization: Bearer {{admin_token}}
```

### Lịch sử attempt

```http
GET {{gateway_url}}/api/v1/payments/{{payment_id}}/attempts
Authorization: Bearer {{admin_token}}
```

### Audit callback

```http
GET {{gateway_url}}/api/v1/payments/{{payment_id}}/callback-audits
Authorization: Bearer {{admin_token}}
```

Thử bằng `customer_token` phải nhận HTTP `403`. Không có token phải nhận HTTP `401`.

## 10. Kiểm tra event RabbitMQ

Sau COD hoặc callback thành công, Payment Service ghi `PaymentSucceeded` vào outbox và phát tới exchange/destination `dynamicmart.events`.

Kiểm tra database:

```sql
SELECT id, order_id, method, timing, status, paid_at
FROM payments
ORDER BY created_at DESC;

SELECT payment_id, provider, provider_reference, status, expires_at
FROM payment_attempts
ORDER BY created_at DESC;

SELECT payment_id, provider, checksum_valid, amount_valid, processed_result, received_at
FROM payment_callback_audits
ORDER BY received_at DESC;

SELECT event_type, status, attempt_count, created_at, published_at
FROM outbox_events
ORDER BY created_at DESC;
```

Khi RabbitMQ hoạt động, outbox thành công phải chuyển từ `PENDING` sang `PUBLISHED`. Event `PaymentDue` dành cho thanh toán online trả sau được Order Service gửi vào destination `payment.due`.

## 11. Các lỗi cần test bắt buộc

| Trường hợp | Kết quả mong đợi |
|---|---|
| Thiếu/sai `X-Internal-Api-Key` | HTTP `401` hoặc `403`. |
| Province/Ward không khớp | HTTP `422`, `GHN_LOCATION_MISMATCH`. |
| Quote dùng lần hai, hết hạn hoặc sai fingerprint | HTTP `409`, `SHIPPING_QUOTE_INVALID`. |
| Cùng `orderId` nhưng đổi amount/method/customer | HTTP `409`, `ORDER_PAYMENT_CONTEXT_CONFLICT`. |
| `PREPAID + COD` | HTTP `422`, `PREPAID_COD_FORBIDDEN`. |
| Tạo online attempt cho COD | HTTP `409`, `PAYMENT_METHOD_NOT_ONLINE`. |
| Callback sai chữ ký | Bị từ chối nhưng vẫn tạo audit. |
| Callback đúng chữ ký nhưng sai amount/reference | Không chuyển Payment sang `PAID`. |
| Callback hợp lệ gửi lại | Idempotent; không phát hai `PaymentSucceeded`. |
| Customer đọc Payment của người khác | HTTP `403`, `PAYMENT_OWNERSHIP_DENIED`. |
| Customer gọi API admin | HTTP `403`. |
