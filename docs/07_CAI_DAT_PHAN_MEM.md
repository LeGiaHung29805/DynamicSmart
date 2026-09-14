# CÀI ĐẶT PHẦN MỀM VÀ HIỆN TRẠNG KHỞI TẠO — DYNAMICMART

> **Cập nhật theo source hiện có:** Tài liệu này phản ánh workspace tại thời điểm khởi tạo. Nó không khẳng định PostgreSQL, RabbitMQ, Docker Compose hay frontend đã được cấu hình khi các file tương ứng chưa tồn tại.

## 1. Công cụ cần có

```text
Git
JDK 17
IntelliJ IDEA hoặc Visual Studio Code + Extension Pack for Java
Docker Desktop (chưa có Docker Compose trong source nhưng cần cho bước hạ tầng)
PostgreSQL client: DBeaver hoặc pgAdmin
Postman hoặc Bruno
Node.js LTS + npm (cần khi khởi tạo frontend Next.js)
```

Kiểm tra môi trường:

```bash
git --version
java -version
docker --version
docker compose version
node -v
npm -v
```

Project hiện dùng Java `17` trong toàn bộ `pom.xml`; không dùng JDK 21 trừ khi cả nhóm nâng version Java đồng loạt và chạy lại build/test.

## 2. Cấu trúc đã có

```text
dynamicmart/
├── api-gateway/
├── services/
│   ├── identity-service/
│   ├── catalog-service/
│   ├── cart-service/
│   ├── order-service/
│   ├── payment-service/
│   └── engagement-service/
├── contracts/                 # Đã tạo nhưng đang trống
├── dynamicmart-frontend/      # Đã tạo nhưng đang trống; chưa phải Next.js app
├── infra/                     # Đã tạo nhưng đang trống; chưa có Docker Compose
└── docs/
```

Mỗi backend là một Maven project độc lập, có Maven Wrapper (`mvnw`/`mvnw.cmd`). Hiện chưa có Maven parent/aggregator ở root, nên build từng project riêng.

Ví dụ trên PowerShell:

```powershell
Set-Location api-gateway
.\mvnw.cmd test

Set-Location ..\services\catalog-service
.\mvnw.cmd test
```

## 3. Quy ước Spring Initializr đang dùng

| Mục | Giá trị hiện có |
|---|---|
| Build tool | Maven |
| Language | Java |
| Java version | 17 |
| Packaging | JAR (Maven default; các `pom.xml` chưa khai báo `<packaging>`) |
| Group | `com.dynamicmart` |
| Version | `0.0.1-SNAPSHOT` |
| Spring Boot | `4.1.1` |
| Spring Cloud BOM | `2025.1.3` |
| File cấu hình | `src/main/resources/application.yaml` |

Tất cả `application.yaml` hiện chỉ có `spring.application.name`; chưa có profile, datasource, Flyway, RabbitMQ, JWT, route Gateway hoặc port.

## 4. Dependency đã có theo project

| Project | Artifact / application name | Dependency hiện có |
|---|---|---|
| `api-gateway` | `api-gateway` / `api-gateway` | Actuator, Spring Security, OAuth2 Resource Server, Spring Cloud Gateway **Server Web MVC**, configuration processor cho compile, các test starter tương ứng |
| `identity-service` | `identity` / `identity` | Actuator, Data JPA, Flyway, Spring Security, Validation, Spring Web MVC, PostgreSQL driver, `spring-cloud-stream`, Lombok, configuration processor và test dependency tương ứng |
| `catalog-service` | `catalog-service` / `catalog-service` | Actuator, Data JPA, Flyway, Validation, Spring Web MVC, PostgreSQL driver, `spring-cloud-stream`, configuration processor và test dependency tương ứng |
| `cart-service` | `cart-service` / `cart-service` | Actuator, Data JPA, Flyway, Validation, Spring Web MVC, PostgreSQL driver, `spring-cloud-stream`, configuration processor và test dependency tương ứng |
| `order-service` | `order-service` / `order-service` | Actuator, Data JPA, Flyway, Validation, Spring Web MVC, PostgreSQL driver, `spring-cloud-stream`, configuration processor và test dependency tương ứng |
| `payment-service` | `payment-service` / `payment-service` | Actuator, Data JPA, Flyway, Validation, Spring Web MVC, PostgreSQL driver, `spring-cloud-stream`, configuration processor và test dependency tương ứng |
| `engagement-service` | `engagement-service` / `engagement-service` | Actuator, Data JPA, Flyway, Validation, Spring Web MVC, PostgreSQL driver, `spring-cloud-stream`, configuration processor và test dependency tương ứng |

`identity-service` có folder tên `identity-service` nhưng Maven artifact và `spring.application.name` đang là `identity`. Trước khi tạo Gateway route, Docker service name và RabbitMQ binding, nhóm phải thống nhất một tên; khuyến nghị chuẩn hóa thành `identity-service`.

## 5. RabbitMQ, Saga và Outbox

Hiện các business service có dependency `spring-cloud-stream`, nhưng **chưa có RabbitMQ binder/starter, cấu hình binder, exchange, queue, retry/DLQ hoặc Docker container RabbitMQ**. Vì vậy RabbitMQ chưa sử dụng được trong source hiện tại.

Khi nhóm bắt đầu tích hợp RabbitMQ, chọn và dùng thống nhất một cách:

```text
Spring Cloud Stream + Rabbit binder
```

Sau đó thêm Rabbit binder phù hợp vào từng service có phát/nhận event, tạo `application-local.yaml`/`application-docker.yaml` cho binding, và khai báo topology trong source hoặc `infra/`.

Saga và Outbox không phải package tải từ Spring Initializr:

- Saga là logic điều phối trong `order-service`.
- Outbox là migration/bảng `outbox_events` và publisher job của từng service phát event.
- Consumer idempotency dùng migration/bảng `processed_events` trong service nhận event.

Chưa có migration, bảng Outbox/processed events hay publisher/consumer trong source hiện tại.

## 6. PostgreSQL và Flyway

Dependency PostgreSQL và Flyway đã có trong sáu business service, nhưng chưa có datasource hoặc migration trong `src/main/resources`.

Khi cấu hình hạ tầng, mỗi service dùng database logic riêng:

```text
identity_db
catalog_db
cart_db
order_db
payment_db
engagement_db
```

Voucher/reservation thuộc `cart_db`; không tạo `promotion_db`. Shipping P1 nằm trong `payment-service`; không tạo `shipping_db` trong phạm vi hiện tại.

Mỗi service cần bổ sung:

```text
src/main/resources/
├── application.yaml
├── application-local.yaml
├── application-docker.yaml
└── db/migration/
    └── V1__initial_schema.sql
```

Không lưu username, password, JWT key, RabbitMQ password hay key VNPay trực tiếp trong Git/YAML. Dùng biến môi trường, ví dụ `${DB_PASSWORD}`.

## 7. API Gateway

Gateway hiện dùng Spring Cloud Gateway **Server Web MVC**, không phải gateway reactive/WebFlux. Gateway không cần JPA, Flyway, PostgreSQL hoặc RabbitMQ.

Các phần chưa có và cần cấu hình trước khi frontend gọi API:

- port Gateway;
- route tới sáu service;
- CORS cho Next.js local;
- OAuth2 Resource Server/JWT edge validation;
- correlation ID và error response chung;
- Actuator health endpoint.

## 8. Frontend

`dynamicmart-frontend/` hiện trống. Khi khởi tạo, dùng Next.js/React/TypeScript/Tailwind trong chính folder này; frontend chỉ gọi API Gateway qua biến không nhạy cảm như `NEXT_PUBLIC_API_BASE_URL`.

Các thư viện có thể thêm sau khi app Next.js được tạo:

```text
TanStack Query
React Hook Form
Zod
Zustand (nếu thật sự cần)
Playwright cho E2E
```

Không đặt JWT service secret, database credential hay VNPay secret ở frontend.

## 9. Hạ tầng Docker

`infra/` hiện trống và chưa có `docker-compose.yml`. Khi tạo, Docker Compose tối thiểu cần:

```text
PostgreSQL
RabbitMQ Management UI
```

Redis là optional cho P0; chỉ thêm khi đã có use case cache/rate limit rõ ràng. Không cần Kubernetes, Kafka, Elasticsearch, Jenkins, Terraform, Ansible, OpenTelemetry đầy đủ hay AI/RAG cho P0.

## 10. Việc cần làm trước khi bắt đầu business logic

1. Chuẩn hóa tên `identity-service` giữa folder, Maven artifact và `spring.application.name`.
2. Chốt Java 17 hoặc nâng đồng loạt lên Java 21; hiện source là Java 17.
3. Khởi tạo Next.js trong `dynamicmart-frontend/`.
4. Tạo `infra/docker-compose.yml` cho PostgreSQL và RabbitMQ Management.
5. Thêm Rabbit binder và cấu hình Cloud Stream trước khi viết event thật.
6. Thêm datasource/Flyway profile và migration `V1` cho từng business service.
7. Tạo migration chuẩn cho `outbox_events` và `processed_events` ở service có event.
8. Cấu hình Gateway route/CORS/JWT trước khi tích hợp frontend.
9. Tạo `.env.example`; không commit `.env` hoặc bất kỳ secret nào.
