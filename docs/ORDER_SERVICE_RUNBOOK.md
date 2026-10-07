# Order Service — Runbook kiểm thử tích hợp và demo

> **Owner:** Hiếu (Người 3)
>
> **Phạm vi:** `services/order-service` và các điểm tích hợp mà Order sử dụng/cung cấp
>
> **Cập nhật:** 2026-10-05
>
> **Nhánh triển khai:** `VanHieu/order-checkout-foundation`

## 1. Mục đích

Tài liệu này giúp một thành viên không viết Order Service vẫn có thể:

1. chạy test mặc định của Order Service;
2. chạy migration và repository integration test trên PostgreSQL test riêng;
3. khởi động Order Service cùng các service phụ thuộc;
4. demo luồng Checkout → Preview → Create Order → Payment → hoàn tất Order;
5. kiểm tra idempotency, phân quyền, khóa đồng thời, Outbox và RabbitMQ;
6. nhận biết lỗi thuộc Order hay thuộc contract của service khác.

Không dùng database `order_db` đang có dữ liệu để chạy integration test. Bộ test PostgreSQL có guard bắt buộc tên database chứa chữ `test`, nhưng người chạy vẫn phải kiểm tra đúng URL trước khi bật test.

## 2. Trạng thái đã xác minh

| Hạng mục | Trạng thái hiện tại |
|---|---|
| Build và test mặc định | `208` test đạt, `0` failure, `0` error, JAR đóng gói thành công |
| Flyway | Có migration từ `V1` đến `V8` |
| PostgreSQL integration suite | `6/6` test đạt trên database test riêng; Flyway V1→V8 và Hibernate validation thành công |
| HTTP contract test | Đã có cho Identity Address, Cart selection/confirmation/voucher, Catalog inventory và Payment/shipping |
| RabbitMQ test binder | Đã xác minh consumer retry, event routing và payload contract trong test |
| RabbitMQ broker thật | Đã đạt smoke test topology, outbound route, Payment consumer idempotency, poison DLQ và phục hồi sau mất broker |
| Runtime local thật | Order + PostgreSQL 16 + RabbitMQ 3.13 đều healthy; Flyway V1→V8, API/JWT/database read và Rabbit consumer đã xác minh |
| E2E nhiều service | Chưa hoàn tất |
| Blocker ngoài Order | Payment `ShippingQuoteResponse` phải trả `requestFingerprint` canonical |

Các thay đổi chưa commit phải được review bằng `git diff` và không được đưa `.idea/compiler.xml` vào commit của Order Service.

## 3. Kiến trúc luồng cần demo

```text
Client
  │ JWT + Idempotency-Key
  ▼
API Gateway
  ▼
Order Service
  ├── Identity: đọc và xác minh Address snapshot
  ├── Cart: đọc selection; preview/reserve/consume/release Voucher; dọn Cart sau OrderConfirmed
  ├── Catalog: đọc Variant; reserve/commit/release Inventory
  ├── Payment: lấy Shipping quote; tạo Payment context/VNPay attempt
  ├── PostgreSQL: Checkout, Order snapshot, Saga, idempotency, Outbox, processed event
  └── RabbitMQ: nhận Payment events; phát Order events và PaymentDue
```

Order không truy cập database của service khác. Khi một bước remote thành công nhưng bước sau thất bại, Saga retry hoặc chạy compensation; đây không phải transaction ACID xuyên nhiều database.

## 4. Điều kiện trước khi chạy

### 4.1. Công cụ

- Java 17.
- PostgreSQL đang chạy.
- RabbitMQ khi chạy broker smoke/E2E event.
- PowerShell trên Windows.
- Các service Identity, Catalog, Cart và Payment ở đúng phiên bản contract tương thích.

Kiểm tra nhanh:

```powershell
java -version
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object LocalPort -In 5432,5672,8081,8082,8083,8084,8085,28080 |
    Select-Object LocalAddress,LocalPort,OwningProcess
```

### 4.2. Cấu hình Order Service

Từ thư mục gốc repository:

```powershell
Copy-Item .\services\order-service\.env.example .\services\order-service\.env
```

Điền giá trị thật vào `.env`; không commit file này. Các biến bắt buộc hoặc quan trọng:

| Nhóm | Biến |
|---|---|
| Database | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| JWT | `JWT_ISSUER`, `JWT_HMAC_SECRET_BASE64` |
| Internal auth | `INTERNAL_API_KEY` |
| RabbitMQ | `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` |
| Service URL | `IDENTITY_SERVICE_BASE_URL`, `CATALOG_SERVICE_BASE_URL`, `CART_SERVICE_BASE_URL`, `PAYMENT_SERVICE_BASE_URL` |
| Timeout | `SERVICE_CONNECT_TIMEOUT`, `SERVICE_READ_TIMEOUT` |
| Worker | `ORDER_SAGA_RECOVERY_*`, `ORDER_OUTBOX_*` nếu cần đổi mặc định |

Ba giá trị `JWT_ISSUER`, `JWT_HMAC_SECRET_BASE64` và `INTERNAL_API_KEY` phải thống nhất với Identity, Gateway và các service liên quan. Không đưa secret thật vào log, ảnh demo hoặc tài liệu Git.

### 4.3. Chạy Order Service local với hạ tầng thật

Compose local chạy Order Service, PostgreSQL và RabbitMQ thật. Order luôn dùng HTTP adapter thật; không có runtime
adapter giả cho Identity, Cart, Catalog hoặc Payment. Vì thế Checkout hoàn chỉnh chỉ chạy khi bốn service đó sẵn sàng.

Từ `services/order-service`:

```powershell
.\scripts\order-local.ps1 up
.\scripts\order-local.ps1 smoke
```

Lệnh `up` build và khởi động ba container. Lệnh `smoke` kiểm tra health, Flyway V8 và RabbitMQ thật; nó không tự
tạo JWT hay dữ liệu nghiệp vụ. Muốn kiểm tra API xác thực, đặt `ORDER_LOCAL_ACCESS_TOKEN` bằng token do Identity
Service thật cấp trước khi chạy `smoke`.

Các endpoint/cổng local:

| Thành phần | Địa chỉ |
|---|---|
| Order Service | `http://127.0.0.1:8084` |
| Order health | `http://127.0.0.1:8084/actuator/health` |
| PostgreSQL | `127.0.0.1:5434` |
| RabbitMQ AMQP | `127.0.0.1:5674` |
| RabbitMQ Management | `http://127.0.0.1:15674` (`order_user/order_password`) |

Lệnh vận hành:

```powershell
.\scripts\order-local.ps1 status
.\scripts\order-local.ps1 logs
.\scripts\order-local.ps1 down
```

Docker Compose dùng credentials cố định chỉ dành cho local và bind port vào `127.0.0.1`. PostgreSQL và RabbitMQ
đều có named volume, nên `down` không xóa dữ liệu. Không chạy `down -v` nếu muốn giữ `order_db`.

## 5. Các mức chạy kiểm thử

### Mức A — kiểm tra độc lập, không cần PostgreSQL/RabbitMQ thật

```powershell
Set-Location .\services\order-service
.\mvnw.cmd clean verify
```

Kết quả chấp nhận:

- build trả exit code `0`;
- `208` test đạt;
- không có failure/error;
- tạo được `target/order-service-0.0.1-SNAPSHOT.jar`.

Nếu số test tăng do bổ sung test mới thì chấp nhận số lớn hơn `208`; không chấp nhận test bị vô hiệu hóa để làm build xanh.

### Mức B — PostgreSQL migration và locking thật

Tạo database test riêng, ví dụ `order_service_test`. Sau đó chạy trong `services/order-service`:

```powershell
$env:ORDER_DATABASE_SMOKE='true'
$env:DB_URL='jdbc:postgresql://localhost:5432/order_service_test'
$env:DB_USERNAME='<test-user>'
$env:DB_PASSWORD='<test-password>'
.\mvnw.cmd '-Dtest=DatabaseMigrationSmokeIT,PostgresRepositoryIntegrationIT' test
```

Bộ test phải xác minh:

- Flyway chạy đủ `V1` → `V8` và Hibernate validate schema;
- unique constraint chặn hai Saga cho cùng Checkout;
- unique constraint chặn cùng `Idempotency-Key` dù khác session;
- pessimistic lock tuần tự hóa hai yêu cầu trên cùng Checkout;
- hai Saga recovery worker claim hai record khác nhau bằng `FOR UPDATE SKIP LOCKED`;
- hai Outbox worker claim hai event khác nhau bằng `FOR UPDATE SKIP LOCKED`.

Sau khi chạy, xóa biến chỉ trong process PowerShell hiện tại nếu không còn dùng:

```powershell
Remove-Item Env:ORDER_DATABASE_SMOKE -ErrorAction SilentlyContinue
Remove-Item Env:DB_URL -ErrorAction SilentlyContinue
Remove-Item Env:DB_USERNAME -ErrorAction SilentlyContinue
Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
```

### Mức C — khởi động ứng dụng thật

Khởi động theo thứ tự để dễ chẩn đoán:

1. PostgreSQL và RabbitMQ.
2. Identity (`8081`).
3. Catalog (`8082`).
4. Cart (`8083`).
5. Payment (`8085`).
6. Order (`8084`).
7. API Gateway (ưu tiên `8080`, dùng `28080` khi `8080` bị chiếm).

Chạy Order:

```powershell
Set-Location .\services\order-service
.\mvnw.cmd spring-boot:run
```

Kiểm tra health trực tiếp:

```powershell
Invoke-RestMethod http://localhost:8084/actuator/health
```

Chỉ tiếp tục E2E khi kết quả có `status = UP`. Startup phải không có lỗi Flyway, schema validation, JWT secret hoặc Rabbit connection.

## 6. Chuẩn bị dữ liệu E2E

Trước demo cần có:

- Customer A và Customer B có access token còn hạn;
- một Admin có access token;
- Customer A có `addressId` hợp lệ với mã tỉnh/phường dùng được cho GHN;
- một Cart của Customer A có ít nhất một dòng được chọn;
- Variant đang bán, có giá, `weightGrams`, `lengthCm`, `widthCm`, `heightCm` hợp lệ và đủ tồn;
- nếu dùng voucher: voucher còn hạn, đúng điều kiện và còn quota;
- Payment trả Shipping quote còn hạn kèm đúng `requestFingerprint`;
- tất cả internal API dùng cùng `INTERNAL_API_KEY`.

Đăng nhập qua Gateway:

```powershell
$gateway = 'http://localhost:8080'
$login = Invoke-RestMethod -Method Post `
    -Uri "$gateway/api/v1/auth/login" `
    -ContentType 'application/json' `
    -Body (@{ email = '<customer-a-email>'; password = '<password>' } | ConvertTo-Json)
$tokenA = $login.accessToken
$headersA = @{ Authorization = "Bearer $tokenA" }
```

Không ghi token thật vào Git. Nếu demo trực tiếp không qua Gateway, đổi `$gateway` thành `http://localhost:8084` cho các API Order, nhưng vẫn phải lấy token từ Identity.

## 7. Kịch bản E2E chuẩn

Các placeholder `<...>` phải thay bằng UUID/dữ liệu thật từ service sở hữu.

Có thể chạy tự động các bước chính bằng script, không truyền secret trực tiếp trên máy dùng chung hoặc khi
terminal đang được ghi hình:

```powershell
Set-Location .\services\order-service
.\scripts\order-e2e-smoke.ps1 `
    -CustomerAEmail '<customer-a-email>' `
    -CustomerAPassword '<password>' `
    -CartId '<cart-id>' `
    -AddressId '<address-id>' `
    -CustomerBEmail '<customer-b-email>' `
    -CustomerBPassword '<password>'
```

Script kiểm tra health, Checkout/Preview, công thức tiền, Create Order replay, key khác bị `409`, đọc Order
và tùy chọn Customer B bị `404`. Các bước bên dưới là bản thủ công để debug từng boundary khi script lỗi.

### Bước 1 — tạo Checkout Session từ Cart

```powershell
$session = Invoke-RestMethod -Method Post `
    -Uri "$gateway/api/v1/checkout/sessions" `
    -Headers $headersA `
    -ContentType 'application/json' `
    -Body (@{ source = 'CART'; cartId = '<cart-id>' } | ConvertTo-Json)
$sessionId = $session.id
```

Kiểm tra response:

- `source = CART` và `cartId` đúng;
- item được lấy server-side, không lấy tên/giá từ request;
- mỗi item có Variant/SKU/quantity/version đúng;
- `expiresAt` còn hạn.

### Bước 2 — chọn địa chỉ và phương thức thanh toán

Ví dụ prepaid VNPay:

```powershell
$updated = Invoke-RestMethod -Method Patch `
    -Uri "$gateway/api/v1/checkout/sessions/$sessionId" `
    -Headers $headersA `
    -ContentType 'application/json' `
    -Body (@{
        addressId = '<address-id>'
        paymentTiming = 'PREPAID'
        paymentMethod = 'VNPAY'
    } | ConvertTo-Json)
```

Các cặp hợp lệ:

- `PREPAID + VNPAY`;
- `POSTPAID + VNPAY`;
- `POSTPAID + COD`;
- `NOT_REQUIRED + FREE` chỉ do backend gán khi tổng thanh toán bằng `0`, client không tự chọn.

### Bước 3 — lấy Checkout Preview

Không dùng voucher thì gửi body rỗng:

```powershell
$preview = Invoke-RestMethod -Method Post `
    -Uri "$gateway/api/v1/checkout/sessions/$sessionId/preview" `
    -Headers $headersA `
    -ContentType 'application/json' `
    -Body (@{} | ConvertTo-Json)
```

Có voucher/service shipping thì body có thể gồm:

```json
{
  "merchandiseVoucherId": "<voucher-id>",
  "shippingVoucherId": "<shipping-voucher-id>",
  "serviceCode": "<shipping-service-code>"
}
```

Kiểm tra:

- địa chỉ thuộc Customer A;
- quote chưa hết hạn và fingerprint khớp input;
- tiền thỏa `finalTotal = itemsSubtotal - productDiscount - orderDiscount + shippingFee - shippingDiscount`;
- mọi số tiền là số nguyên VND và không âm;
- Preview không giữ tồn và chưa tạo Order/Payment.

Nếu nhận `502 INVALID_SHIPPING_QUOTE_CONTRACT` vì thiếu `requestFingerprint`, đây là blocker contract của Payment; không sửa Order để tự tính hoặc bỏ kiểm tra fingerprint.

### Bước 4 — tạo Order với idempotency key

```powershell
$createKey = [guid]::NewGuid().ToString()
$createHeaders = @{
    Authorization = "Bearer $tokenA"
    'Idempotency-Key' = $createKey
}
$order = Invoke-RestMethod -Method Post `
    -Uri "$gateway/api/v1/checkout/sessions/$sessionId/orders" `
    -Headers $createHeaders
$orderId = $order.orderId
```

Kiểm tra lần đầu:

- HTTP `201`;
- chỉ có một Order và một Saga;
- snapshot item/address/voucher/shipping không phụ thuộc dữ liệu thay đổi sau đó;
- inventory/voucher reservation được checkpoint;
- Order có `paymentId` khi cần Payment;
- Outbox có event cần phát.

Gửi lại đúng `sessionId` và đúng `Idempotency-Key`:

```powershell
$replay = Invoke-RestMethod -Method Post `
    -Uri "$gateway/api/v1/checkout/sessions/$sessionId/orders" `
    -Headers $createHeaders
```

Kết quả phải là HTTP `200`, `replay = true`, cùng `orderId`, `sagaId` và `paymentId`; không tạo thêm Order, reservation hoặc Payment context.

Gửi key khác cho cùng Checkout phải bị từ chối `409`, không tạo Order thứ hai.

### Bước 5 — xem Order và timeline

```powershell
$detail = Invoke-RestMethod -Method Get `
    -Uri "$gateway/api/v1/orders/$orderId" `
    -Headers $headersA
$timeline = Invoke-RestMethod -Method Get `
    -Uri "$gateway/api/v1/orders/$orderId/timeline" `
    -Headers $headersA
```

Customer B dùng token của B để gọi cùng URL phải nhận `404 ORDER_NOT_FOUND`, không được biết Order của A có tồn tại.

### Bước 6 — Payment event và vòng đời Order

Với prepaid, Payment Service phát một trong `PaymentSucceeded`, `PaymentFailed`, `PaymentExpired`. Order consumer phải:

- kiểm tra event ID, producer, version, order/payment ID, amount, timing, method và correlation;
- chỉ xử lý một lần nếu cùng event được gửi lặp;
- success thì xác nhận Order và commit/consume reservation;
- failed/expired thì hủy Order và release reservation theo policy;
- event sai contract phải retry, sau số lần cấu hình phải vào DLQ.

Với Order đã xác nhận, Admin thực hiện tuần tự bằng key mới cho mỗi thao tác:

```text
POST /api/v1/orders/admin/{orderId}/pack
POST /api/v1/orders/admin/{orderId}/ship
POST /api/v1/orders/admin/{orderId}/handover
```

Customer A xác nhận đã nhận:

```text
POST /api/v1/orders/{orderId}/received
```

Mỗi request lifecycle cần header `Idempotency-Key: <UUID>`. Gửi lặp cùng key phải trả lại kết quả cũ và không thêm transition/event lần hai.

## 8. Kiểm tra database sau E2E

Chỉ đọc dữ liệu trong database đúng môi trường demo:

```sql
SELECT id, status, completed_order_id, expires_at
FROM checkout_sessions
WHERE id = '<checkout-session-id>';

SELECT id, order_number, customer_id, status, final_total_vnd, payment_due_at
FROM orders
WHERE id = '<order-id>';

SELECT id, checkout_session_id, idempotency_key, status, current_step, payment_id
FROM order_sagas
WHERE checkout_session_id = '<checkout-session-id>';

SELECT from_status, to_status, actor_type, reason, created_at
FROM order_status_history
WHERE order_id = '<order-id>'
ORDER BY created_at;

SELECT id, event_type, status, attempt_count, processing_owner,
       processing_lease_until, published_at
FROM outbox_events
WHERE aggregate_id = '<order-id>'
ORDER BY created_at;
```

Điều kiện chấp nhận:

- một Checkout chỉ có tối đa một Order/Saga;
- replay không tăng số Order hoặc Saga;
- history đi theo state machine, không nhảy trạng thái tùy ý;
- Outbox thành công ở `PUBLISHED`; lỗi tạm thời quay lại `PENDING` với backoff; lỗi lần thứ 10 ở `FAILED`;
- event đang gửi có `PROCESSING`, owner và lease; worker khác chỉ reclaim sau khi lease hết hạn.

## 9. RabbitMQ smoke checklist

Sau khi Order chạy với broker thật, kiểm tra RabbitMQ Management UI:

- destination/exchange `dynamicmart.events` tồn tại;
- consumer group `order-service` có queue/binding tương ứng;
- DLX `dynamicmart.events.dlx` tồn tại;
- DLQ `dynamicmart.events.order-service.dlq` bind bằng routing key `order-service.payment.failed`;
- destination `payment.due` tồn tại khi có event postpaid VNPay;
- Order consumer đang connected.

Ba ca smoke bắt buộc:

1. Phát một Payment event hợp lệ: Order đổi trạng thái đúng và `processed_events` có đúng một record.
2. Phát lại chính event đó: trạng thái, history, reservation và Outbox không tăng lần hai.
3. Phát poison message đúng route nhưng sai contract: consumer retry tối đa 5 lần rồi message xuất hiện trong DLQ, không requeue vô hạn.

Kết quả gần nhất ngày 2026-10-03 trên RabbitMQ 3.13.7 chạy bằng Docker:

- `dynamicmart.events`, queue group `order-service`, DLX/DLQ và binding được provision đúng; consumer có một kết nối hoạt động;
- Outbox thường đi `dynamicmart.events`, `PaymentDue` đi exchange topic bền vững `payment.due`; cả hai chuyển `PENDING → PUBLISHED`, không còn owner/lease;
- `PaymentSucceeded` thật chuyển Order `DELIVERED → COMPLETED`; phát lại cùng `eventId` vẫn chỉ có một `processed_events`, một history và một Outbox event;
- poison JSON được thử đủ 5 lần rồi republish vào `dynamicmart.events.order-service.dlq` với payload và metadata lỗi gốc;
- khi broker bị dừng, Outbox giữ `PENDING` và tăng attempt/backoff; sau khi broker khởi động lại, consumer tự nối lại, topology tự tạo lại và event chuyển `PUBLISHED`.

Ca mất broker:

1. Dừng RabbitMQ trong lúc Outbox còn `PENDING`.
2. Xác minh Order API không mất record Outbox và publisher tăng attempt/backoff.
3. Khởi động RabbitMQ lại.
4. Xác minh event được publish và chuyển `PUBLISHED`.

Mô hình là at-least-once: nếu broker nhận message nhưng Order dừng trước khi đánh dấu `PUBLISHED`, event có thể được gửi lại. Consumer bắt buộc idempotent; không kết luận duplicate delivery là lỗi nếu duplicate side effect đã được chặn.

## 10. Các ca nghiệm thu bắt buộc

| Ca | Kết quả mong đợi |
|---|---|
| Cart trống hoặc selection không hợp lệ | `422`, không tạo session/order |
| Catalog/Cart/Identity/Payment không kết nối được | `503` với mã dependency rõ ràng |
| Dependency trả payload sai | `502` contract invalid, không lưu dữ liệu bịa |
| Address của Customer khác | từ chối, không lộ snapshot |
| Quote hết hạn/sai fingerprint | không cho tạo Order |
| Hai request cùng idempotency key | một tác động, request sau replay |
| Hai key khác nhau trên cùng Checkout | một Order; request còn lại `409` |
| Hai worker recovery/outbox | không claim cùng record khi lease còn hạn |
| Payment event lặp | một transition/side effect |
| Customer B đọc Order A | `404 ORDER_NOT_FOUND` |
| Customer gọi API Admin | `403` |
| Access token thiếu/hỏng/hết hạn | `401` |
| Payment success và received khác thứ tự | state machine giữ kết quả hợp lệ, không double effect |

## 11. Cách phân loại lỗi tích hợp

| Dấu hiệu | Owner xử lý đầu tiên |
|---|---|
| Sai tính tiền, Saga, state machine, Order snapshot, Outbox Order | Hiếu / Order Service |
| Address ownership/snapshot sai ngay tại Identity endpoint | Owner Identity |
| Cart selection/version/voucher contract sai | Owner Cart |
| Variant/package/inventory reservation sai | Owner Catalog |
| Quote thiếu fingerprint, Payment context/event sai | Owner Payment |
| Route, JWT edge, CORS hoặc session validation sai | Owner phần chung/Gateway |
| Exchange/queue convention không thống nhất | Nhóm chốt contract chung; owner service sửa phần mình |

Nguyên tắc xử lý conflict: chủ sở hữu service có ưu tiên cao nhất trong module của họ. Hiếu chỉ sửa `order-service` và tài liệu thuộc phạm vi Order; thay đổi contract service khác được ghi thành yêu cầu tích hợp, không tự sửa chéo nếu chưa phối hợp.

## 12. Definition of Done của phần Hiếu

Phần Order chỉ được xem là hoàn tất khi:

- `clean verify` đạt;
- PostgreSQL V1→V8 và toàn bộ repository IT đạt trên database test riêng;
- RabbitMQ broker smoke đạt, gồm duplicate và DLQ;
- E2E thật đạt với Identity, Catalog, Cart và Payment;
- Payment trả fingerprint canonical và Order xác minh được;
- demo idempotency, ownership, compensation, lifecycle và Outbox có bằng chứng;
- tài liệu tiến độ/runbook khớp source;
- thay đổi được chia commit nhỏ, rõ ràng và không chứa file IDE/secret.
