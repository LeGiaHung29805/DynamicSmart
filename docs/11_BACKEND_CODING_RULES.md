# LUẬT BACKEND — DYNAMICMART

> Áp dụng cho `identity-service`, `catalog-service`, `cart-service`, `order-service`, `payment-service`, `engagement-service` và `api-gateway`. Mục tiêu là mã dễ tìm, không lộ dữ liệu nội bộ, không phá ranh giới service và ít xung đột khi làm việc nhóm.

## 1. Kiến trúc và ranh giới bắt buộc

- Mỗi business service là một Spring Boot/Maven project độc lập, đóng gói `JAR`, Java 17 và cấu hình YAML.
- Frontend chỉ gọi API Gateway. Gateway chỉ làm route, CORS, xác thực ở edge, correlation ID, rate limit và chuẩn hóa lỗi; **không chứa business logic, JPA, Flyway hoặc database**.
- Service chỉ đọc/ghi database do mình sở hữu. Không dùng foreign key, JDBC, repository hoặc entity của service khác.
- Cần phản hồi ngay thì gọi REST qua contract đã chốt; thông báo kết quả nghiệp vụ liên service thì phát event qua RabbitMQ/Outbox.
- Không copy bảng hoặc entity của service khác. Dữ liệu liên service cần cho lịch sử phải được snapshot hoặc xây read model từ event.

## 2. Cấu trúc source chuẩn trong mỗi business service

Giữ package gốc theo service, ví dụ `com.dynamicmart.catalog_service`. Các business service dùng cùng một **khung tầng ngang** để mọi người mới vào dự án nhìn là biết vị trí của Controller, DTO, Entity, Repository và Service. Khi một nghiệp vụ lớn, có thể tạo package con bên trong từng tầng, ví dụ `controller/product`, `service/inventory`.

```text
src/main/java/com/dynamicmart/catalog_service/
├── CatalogServiceApplication.java
├── config/                         # ConfigurationProperties, Security, Web, Jackson
├── common/
│   ├── api/                        # ApiResponse, PageResponse, ErrorResponse
│   ├── exception/                  # BusinessException, GlobalExceptionHandler
│   └── util/                       # Hàm thật sự dùng chung, không chứa nghiệp vụ
├── controller/                     # HTTP controller, có thể chia product/, inventory/...
├── dto/
│   ├── request/                    # CreateProductRequest, UpdateProductRequest...
│   └── response/                   # ProductDetailResponse, ProductSummaryResponse...
├── entity/                         # JPA entity, enum trạng thái, aggregate local
├── repository/                     # Spring Data JPA repository/projection/query local
├── service/                        # Use case, business rule, transaction
├── mapper/                         # Entity <-> DTO; không query DB
├── client/                         # REST client tới service/nhà cung cấp khác, theo contract
├── messaging/
│   ├── consumer/                   # RabbitMQ/Cloud Stream consumer
│   └── producer/                   # Chỉ publisher kỹ thuật; business event qua Outbox
├── outbox/                         # OutboxEvent, repository, publisher job
└── exception/                      # Exception riêng của service nếu không dùng common
```

Các package tầng gốc đã được tạo sẵn kèm `.gitkeep` để mọi người clone về nhìn thấy cấu trúc. Không đặt file nghiệp vụ mới ngoài các tầng này. `api-gateway` là ngoại lệ: chỉ có `config`, `filter`, `routing`, `security`, `exception`; Gateway không có `entity`, `repository` hay `service` nghiệp vụ.

### Ý nghĩa từng tầng

| Tầng | Được làm | Không được làm |
|---|---|---|
| `controller` | Nhận HTTP, `@Valid`, lấy principal, gọi service, trả response/status | Query DB, transaction, tính tiền, map thủ công dài dòng |
| `dto/request`, `dto/response` | DTO phục vụ contract HTTP | Dùng làm JPA entity hoặc tái sử dụng làm event nội bộ |
| `service` | Điều phối use case, business rule, authorization, transaction, gọi repository/client | Nhận `HttpServletRequest` hay trả `ResponseEntity` |
| `entity` | Trạng thái nghiệp vụ local, enum, invariant nhỏ | Lộ ra API hoặc phụ thuộc controller/DTO |
| `repository` | Truy vấn và lưu aggregate local | Logic nghiệp vụ, gọi service khác |
| `mapper` | Chuyển đổi DTO/domain/entity rõ ràng | Query DB hoặc tự ra quyết định business rule |
| `query` | Filter, Specification, projection, tối ưu read model local | Nhét vào controller hoặc gọi DB service khác |
| `client` | REST adapter, timeout, map lỗi kỹ thuật | Chứa fallback tự ý thay đổi nghiệp vụ |
| `messaging/outbox` | Phát/nhận event idempotent, retry/DLQ | Ghi trực tiếp database của service nhận |

## 3. DTO, Entity và Mapping

- **Không bao giờ** trả JPA Entity trực tiếp từ controller và không nhận Entity từ request body.
- Mỗi API dùng DTO theo mục đích: `CreateXRequest`, `UpdateXRequest`, `XDetailResponse`, `XSummaryResponse`. Không dùng một DTO cho tạo, sửa, chi tiết và danh sách nếu trường/validation khác nhau.
- DTO request dùng Bean Validation (`@NotBlank`, `@Positive`, `@Email`, `@Size`...). Controller bắt buộc có `@Valid`.
- Entity chứa các cột persistence; response chỉ chứa dữ liệu client được phép thấy. Tuyệt đối không trả password hash, refresh token, secret, internal note hoặc dữ liệu của customer khác.
- Mapping là tầng bắt buộc. Dùng mapper viết tay ngắn, rõ ràng hoặc MapStruct khi cả nhóm đã thêm dependency/cấu hình thống nhất. Không để mapping dài trong controller.
- Mapper không tự query database. Khi cần resolve `brandId`, `variantId`, principal hoặc kiểm tra ownership, service phải tìm/validate trước rồi truyền object hợp lệ vào mapper.
- Event payload là contract riêng, có version; không phát Entity hoặc REST response làm event.

## 4. Repository, truy vấn và transaction

- Repository chỉ làm persistence local. Tên method phải diễn tả rõ điều kiện, ví dụ `findByPublicIdAndCustomerId(...)`.
- Dùng `Optional` cho truy vấn một bản ghi có thể không tồn tại; service chuyển nó thành exception nghiệp vụ rõ nghĩa.
- List/search bắt buộc phân trang khi dữ liệu có thể lớn. Dùng `Pageable`, sort allow-list; không nhận tên cột sort tự do từ client.
- Tránh N+1 bằng fetch join, entity graph hoặc projection. Đo và kiểm tra SQL trước khi thêm cache tùy tiện.
- Transaction nằm tại application service. Đọc dùng `@Transactional(readOnly = true)` khi phù hợp; command dùng `@Transactional` ở boundary use case.
- Không giữ transaction qua HTTP client, payment provider hoặc message broker. Với event, ghi business data và `outbox_events` trong **một local transaction**.
- Không xóa cứng Product/Variant đã đi vào Order; vô hiệu hóa theo business rule và giữ snapshot lịch sử.
- Không sửa migration Flyway đã được áp dụng. Mọi thay đổi schema tạo file migration mới, ví dụ `V12__add_payment_expire_at.sql`.

## 5. Controller và REST API

- URL version chung qua Gateway, ví dụ `/api/v1/products`; dùng danh từ, số nhiều, kebab-case khi cần. Không đưa động từ như `/createProduct` vào URL.
- Dùng HTTP status đúng: `201` khi tạo, `204` khi xóa/không body, `400` request không hợp lệ, `401` chưa xác thực, `403` không có quyền, `404` không tìm thấy, `409` conflict/idempotency/state không hợp lệ.
- Mọi response/error theo envelope chung đã chốt trong API contract. Không để từng service tự trả shape lỗi khác nhau.
- Validate ownership trong service: customer chỉ đọc/sửa dữ liệu của mình; ID nằm trong URL không đồng nghĩa với được phép truy cập.
- Controller chỉ gọi use case của chính service. Không gọi repository, consumer hoặc payment adapter trực tiếp.
- API tạo Order, payment callback/IPN, reserve/release và consumer event phải có idempotency theo contract.

## 6. Exception, logging và audit

- Chỉ service/domain ném exception nghiệp vụ có mã ổn định, ví dụ `CART_ITEM_NOT_FOUND`, `ORDER_STATE_INVALID`, `GHN_QUOTE_EXPIRED`.
- `GlobalExceptionHandler` là nơi duy nhất đổi exception thành HTTP error response. Không `try/catch` mọi nơi rồi trả `null` hoặc `500` chung chung.
- Log có `correlationId`, `service`, `eventId`/`orderId` khi có. Không log password, token, OTP, chữ ký VNPay, key GHN, thông tin thẻ hoặc body nhạy cảm.
- Các thay đổi admin quan trọng (khóa user, cập nhật role, thay đổi promotion) ghi audit local theo thiết kế database.

## 7. Security và configuration

- Configuration mặc định đặt trong `application.yaml`; cấu hình theo môi trường đặt ở `application-local.yaml`, `application-docker.yaml`, `application-prod.yaml`.
- Binding cấu hình bằng `@ConfigurationProperties`; không rải `@Value` cho một nhóm cấu hình và không hard-code URL, secret, port hay credential.
- Secret chỉ đến từ biến môi trường/secret manager. Commit `.env.example`, không commit `.env`, private key JWT, VNPay secret hay GHN token.
- Gateway xác thực JWT ở edge; business service vẫn phải kiểm tra role/ownership theo dữ liệu principal/contract được tin cậy. Không tin `customerId`, `price`, `shippingFee`, role do frontend gửi.
- Chỉ admin mới dùng endpoint quản trị. Cần có test cho `401`, `403` và truy cập chéo customer.

## 8. REST client, RabbitMQ, Saga và Outbox

- REST client đặt trong `client/`, có timeout và map lỗi kỹ thuật thành lỗi domain/application phù hợp. Không gọi service khác bằng URL hard-code.
- Không phát event trực tiếp sau khi ghi database. Lưu Outbox trong cùng transaction, publisher mới gửi broker và đánh dấu trạng thái.
- Consumer lưu/kiểm tra `processed_events` trước khi thực thi side effect; event trùng phải an toàn.
- Event envelope dùng `eventId`, `eventType`, `eventVersion`, `producer`, `aggregateId`, `occurredAt`, `correlationId`, `payload` theo tài liệu chung.
- Order Service điều phối Saga Create Order. Payment Service không cập nhật Order DB; Catalog/Cart không cập nhật Order DB.
- Không dùng distributed transaction/2PC. Compensation phải idempotent: reserve/commit/release inventory và voucher.

## 9. Testing bắt buộc

- Unit test cho service, state transition, mapper có logic, validation, ownership và các nhánh lỗi quan trọng.
- Repository/integration test cho query, index/constraint quan trọng, Flyway migration và transaction/concurrency cần thiết.
- Controller test cho validation, authorization, HTTP status/error envelope.
- Contract test cho REST/event liên domain khi endpoint/event được chia sẻ.
- Event consumer test event trùng, thứ tự bất lợi và retry; payment test callback trùng, checksum/amount sai và callback đến muộn.
- Mỗi pull request thay business rule, API/event hay migration phải bổ sung test phù hợp.

## 10. Quy tắc review để tránh ghi đè

- Một nhánh chỉ tập trung một domain/use case: `feature/<domain>-<task>`.
- Không sửa package của service khác hoặc contract chung nếu chưa thông báo/review người phụ trách.
- Thay đổi REST, event, schema, status hoặc DTO public phải cập nhật tài liệu contract trước hoặc cùng pull request.
- Không trộn refactor lớn với thay đổi nghiệp vụ. Không đổi tên hàng loạt khi một domain khác đang tích hợp.
- Build/test tối thiểu service bị ảnh hưởng trước khi mở pull request.
