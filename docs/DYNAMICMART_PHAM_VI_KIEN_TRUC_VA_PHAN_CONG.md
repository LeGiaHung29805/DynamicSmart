# DynamicMart — Phạm vi chức năng, kiến trúc, công nghệ và phân công đề xuất

> **Trạng thái:** Tài liệu tham khảo của phương án trước. Phạm vi triển khai hiện hành là bộ `00_OVERVIEW.md`–`07_CAI_DAT_PHAN_MEM.md`; bộ đó ưu tiên khi khác biệt về số service, database, P0/P1 hoặc owner.

> **Mục đích:** Tài liệu này là phạm vi đề xuất trước khi nhóm bắt đầu lập trình. Nó thay thế cách hiểu mơ hồ rằng có tài liệu là tính năng đã hoàn thành. Mỗi tính năng chỉ được xem là hoàn thành khi có dữ liệu, API, giao diện, kiểm thử và kịch bản demo tương ứng.

## 1. Tóm tắt đề tài

DynamicMart là nền tảng thương mại điện tử B2C đa ngành hàng. Hệ thống cho phép khách hàng tìm sản phẩm theo thuộc tính động, thêm vào giỏ, áp dụng mã giảm giá, lấy phí giao hàng từ GHN, thanh toán, theo dõi đơn và đánh giá sản phẩm. Quản trị viên hoặc người bán có thể quản lý hàng hóa, tồn kho, khuyến mãi, đơn hàng và báo cáo.

Kiến trúc đề xuất là **microservices** (kiến trúc gồm các dịch vụ độc lập). Mỗi dịch vụ sở hữu dữ liệu của mình, giao tiếp qua API nội bộ hoặc sự kiện RabbitMQ. Không có dịch vụ nào được truy vấn trực tiếp cơ sở dữ liệu của dịch vụ khác.

```text
Giao diện Next.js
       |
       v
Cổng API (API Gateway — cổng nhận và điều hướng yêu cầu)
       |
       +-- Identity Service       -> identity_db
       +-- Catalog Service        -> catalog_db
       +-- Cart Service           -> cart_db
       +-- Promotion Service      -> promotion_db
       +-- Order Service          -> order_db
       +-- Payment Service        -> payment_db
       +-- Shipping Service       -> shipping_db
       +-- Engagement Service     -> engagement_db

Các dịch vụ phát/nhận sự kiện qua RabbitMQ.
```

## 2. Nguyên tắc bắt buộc

- Mỗi dịch vụ chỉ đọc/ghi database do chính nó sở hữu.
- Không tạo khóa ngoại giữa hai database khác nhau; chỉ lưu mã tham chiếu ổn định như UUID.
- Không tin giá, tồn kho, tổng tiền, quyền hay trạng thái do giao diện gửi lên.
- Tiền VND lưu bằng số nguyên `BIGINT`; không dùng `float` hoặc `double`.
- Mọi thao tác quan trọng có thể gửi lại phải có **idempotency key** (mã định danh yêu cầu; gửi lại cùng mã không tạo tác động lần hai).
- Các dịch vụ tiêu thụ sự kiện phải xử lý lặp an toàn (**idempotent consumer** — event gửi lại không làm tăng doanh thu, gửi mail hay trừ kho lần hai).
- Đơn hàng luôn lưu bản chụp dữ liệu tại thời điểm tạo: tên hàng, giá, SKU, thuộc tính, địa chỉ, phí giao và giảm giá.
- API nội bộ phải được xác thực bằng mã dịch vụ hoặc chữ ký; không mở ra ngoài qua cổng API công khai.
- Mọi thay đổi trạng thái Order, Payment, Shipment và Inventory Reservation phải đi qua service chuyên trách, không nhận trạng thái tùy ý từ client.

## 3. Phạm vi theo mức ưu tiên

### P0 — bắt buộc cho bản demo nghiệp vụ chính

- Đăng ký, đăng nhập, quyền khách hàng/người bán/quản trị viên.
- Danh mục, thuộc tính động, sản phẩm, ảnh, tìm kiếm và lọc.
- Tồn kho, giữ chỗ tồn kho, chống bán vượt tồn.
- Giỏ hàng cho khách đã đăng nhập.
- Checkout, tạo đơn, bản chụp Order, chống tạo đơn trùng.
- Mã giảm giá cơ bản, trong đó có mã giảm phí giao hàng.
- Báo giá GHN theo địa chỉ và trọng lượng; báo giá có thời hạn.
- COD và VNPay Sandbox, callback có kiểm tra chữ ký/số tiền/idempotency.
- Saga (chuỗi giao dịch giữa nhiều service) cho reserve tồn, Order và Payment.
- RabbitMQ, Transactional Outbox (lưu event trong cùng transaction trước khi gửi), xử lý event lặp.
- Review sau khi đơn hoàn thành; doanh thu và best seller cơ bản.
- Docker Compose, OpenAPI/Swagger, kiểm thử tích hợp và luồng E2E.

### P1 — hoàn thiện sau khi P0 ổn định

- Giỏ hàng khách vãng lai và gộp giỏ khi đăng nhập.
- Campaign (chiến dịch khuyến mãi), ngân sách và đối tác tài trợ.
- Wishlist, thông báo trong hệ thống, email có retry.
- Tạo vận đơn GHN, tracking và webhook vận chuyển.
- Hủy đơn nâng cao, hoàn hàng và hoàn tiền.
- Báo cáo theo danh mục, theo thời gian, doanh thu ròng sau hoàn tiền.
- Cảnh báo tồn kho thấp.

### P2 — chỉ làm khi P0/P1 đã được kiểm thử ổn định

- Đăng nhập Google OAuth.
- ProductVariant nâng cao hoặc nhiều kho.
- Nhiều nhà vận chuyển.
- Gợi ý sản phẩm.
- Chat trực tiếp.
- Trợ lý AI/RAG.

## 4. Chức năng chi tiết theo dịch vụ

### 4.1. Dịch vụ tài khoản — Identity Service

**Owner:** Người 2. **Database:** `identity_db`.

#### Chức năng khách hàng

- Đăng ký; đăng ký công khai chỉ tạo vai trò `CUSTOMER`.
- Đăng nhập, đăng xuất.
- Cấp access token (mã xác thực ngắn hạn) và refresh token (mã xin lại access token).
- Xoay vòng, thu hồi và lưu dạng băm refresh token.
- Xem/sửa hồ sơ; đổi mật khẩu.
- Quản lý sổ địa chỉ: thêm, sửa, xóa, đặt mặc định.
- Lưu mã tỉnh/thành, quận/huyện và phường/xã để báo giá GHN.
- Chỉ chủ tài khoản được đọc hoặc sửa địa chỉ/hồ sơ của mình.

#### Chức năng quản trị

- Xem và khóa/mở khóa tài khoản khi có chính sách rõ ràng.
- Gán vai trò `SELLER` hoặc `ADMIN` bằng API quản trị riêng.
- Theo dõi dấu vết thay đổi quyền quan trọng.

#### Bảng chính

`users`, `roles`, `user_roles`, `user_addresses`, `refresh_tokens`, `password_reset_tokens`, `outbox_events`.

#### Kiểm thử bắt buộc

- Email trùng, mật khẩu sai, token hết hạn/revoke.
- Public register không thể gửi role quản trị.
- Khách A không đọc/sửa địa chỉ của khách B.
- Refresh token không được lưu dạng thô.

### 4.2. Dịch vụ danh mục và tồn kho — Catalog Service

**Owner:** Người 1. **Database:** `catalog_db`.

#### Danh mục và thuộc tính động

- Tạo danh mục cây cha–con; chặn vòng lặp.
- Bật/tắt danh mục; danh mục tắt không có sản phẩm công khai mới.
- Tạo thuộc tính: chữ, số nguyên, số thập phân, đúng/sai, chọn một, chọn nhiều.
- Tạo lựa chọn cho thuộc tính như màu sắc, dung lượng, chất liệu.
- Gắn thuộc tính vào danh mục; hỗ trợ kế thừa từ danh mục cha.
- Thiết lập bắt buộc, cho phép tìm kiếm/lọc, đơn vị đo, thứ tự hiển thị và quy tắc validation.
- Phiên bản schema thuộc tính để thay đổi cấu hình sau này không làm dữ liệu cũ vô nghĩa.

#### Sản phẩm và giao diện quản trị

- Tạo, sửa, ẩn/xóa mềm sản phẩm.
- Mỗi đơn vị có thể mua phải có SKU duy nhất.
- Quản lý tên, slug, mô tả, giá, trạng thái, ngày công khai và thuộc tính động JSONB.
- Tải ảnh, sắp xếp ảnh, đặt ảnh chính, xóa ảnh an toàn.
- Danh sách sản phẩm, tìm kiếm từ khóa, lọc theo category/giá/thuộc tính, sắp xếp.
- Trang chi tiết có ảnh, thông số động, giá, tồn và review.

#### Quyết định về ProductVariant

P0 có thể dùng một Product là một đơn vị bán được, có một SKU và một tồn kho. Nếu muốn bán áo có nhiều size/màu với giá hoặc tồn riêng, ProductVariant (phiên bản hàng bán) là bắt buộc. Nhóm phải chốt **một** mô hình trước khi viết Cart và Checkout; không được để service hiểu `product_id` và `variant_id` khác nhau.

#### Tồn kho

- Tồn sẵn, tồn giữ chỗ, ngưỡng sắp hết hàng.
- Nhập kho/điều chỉnh kho và lịch sử biến động.
- Reservation: giữ, xác nhận, giải phóng và hết hạn giữ chỗ.
- Không oversell (bán vượt tồn), không tồn âm.
- Khóa bản ghi hoặc cập nhật nguyên tử cho tình huống nhiều khách mua hàng cuối.
- Mọi reserve/commit/release xử lý lặp an toàn.

#### Bảng chính

`categories`, `attributes`, `attribute_options`, `category_attributes`, `products`, `product_images`, `inventories`, `inventory_reservations`, `inventory_transactions`, `outbox_events`, `processed_events`.

### 4.3. Dịch vụ giỏ hàng — Cart Service

**Owner:** Người 2. **Database:** `cart_db`.

- Xem giỏ hàng đang hoạt động.
- Thêm, đổi số lượng, xóa mặt hàng.
- Chọn/bỏ chọn các dòng thanh toán.
- Một khách chỉ có một giỏ đang hoạt động; không tạo dòng trùng cùng SKU.
- Không nhận giá từ giao diện; giá snapshot chỉ dùng để hiển thị.
- Kiểm tra lại trạng thái bán được khi thêm/cập nhật.
- Hiển thị tạm tính, ảnh, lỗi hết hàng và giỏ trống.
- Guest Cart (giỏ khách chưa đăng nhập) và gộp giỏ: P1.

#### Bảng chính

`carts`, `cart_items`.

### 4.4. Dịch vụ khuyến mãi — Promotion Service

**Owner:** Người 5. **Database:** `promotion_db`.

> Voucher giảm phí giao hàng thuộc Promotion Service. Shipping Service chỉ trả phí gốc; Promotion Service quyết định số tiền giảm.

#### Voucher P0

- Mã giảm phần trăm.
- Mã giảm số tiền cố định.
- Mã miễn/giảm phí ship.
- Ngày bắt đầu/kết thúc, trạng thái bật/tắt.
- Giá trị Order tối thiểu, mức giảm tối đa.
- Tổng lượt dùng và lượt dùng tối đa mỗi khách.
- Áp dụng theo toàn đơn, danh mục hoặc sản phẩm khi nhóm có đủ thời gian.
- Kiểm tra server-side, không tin giảm giá do client gửi.

#### Campaign P1

- Tạo chiến dịch, thời gian, trạng thái và nhiều voucher trong một campaign.
- Rule chồng khuyến mãi: loại trừ hoặc cho phép cộng dồn.
- Ngân sách campaign và phân bổ chi phí giữa shop/đối tác.
- Voucher dành cho khách mới, khách VIP hoặc danh sách khách được chọn.

#### Vòng đời sử dụng

- Validate: kiểm tra tính hợp lệ trước tạo Order.
- Reserve: giữ tạm lượt dùng trong Checkout.
- Consume: ghi nhận dùng khi Order thành công.
- Release: trả lại lượt dùng khi tạo đơn thất bại, Order bị hủy hoặc reservation hết hạn.
- Chặn hai khách cùng dùng lượt voucher cuối.
- Trả cho Order: `merchandise_discount` (giảm tiền hàng), `shipping_discount` (giảm phí giao), `reservation_id` và snapshot rule.

#### Bảng chính

`campaigns`, `vouchers`, `voucher_rules`, `voucher_reservations`, `voucher_redemptions`, `sponsor_fundings` (P1), `outbox_events`, `processed_events`.

### 4.5. Dịch vụ giao hàng — Shipping Service

**Owner:** Người 4. **Database:** `shipping_db`.

#### Báo giá P0

- Chuẩn hóa địa chỉ sang mã hành chính GHN.
- Nhận tổng trọng lượng/kích thước từ Catalog qua contract.
- Gọi GHN để lấy phí giao gốc, dịch vụ và thời gian dự kiến.
- Lưu quote gồm provider, service, `base_fee`, ETA, thời gian hết hạn.
- Dùng fingerprint (dấu vết gắn quote với đúng user, địa chỉ, mặt hàng và số lượng).
- Từ chối quote hết hạn, sai chủ sở hữu hoặc sai fingerprint.
- Đổi địa chỉ/giỏ/số lượng thì bắt buộc quote lại.
- Timeout/lỗi GHN trả lỗi rõ; không tạo phí giả bằng `0`.

#### Shipment P1

- Tạo vận đơn, lưu tracking code.
- Theo dõi timeline giao hàng.
- Nhận webhook GHN; xác thực callback.
- Retry khi provider lỗi; retry không tạo vận đơn trùng.
- Provider lỗi không rollback Order đã commit.

#### Bảng chính

`shipping_quotes`, `shipments`, `shipment_events`, `provider_request_logs`, `outbox_events`, `processed_events`.

### 4.6. Dịch vụ Checkout, đơn hàng và Saga — Order Service

**Owner:** Người 3. **Database:** `order_db`.

Checkout là use case của Order Service, không cần database/service riêng.

#### Checkout

- Checkout từ Cart; Buy Now (mua ngay) là P1 nếu P0 chưa ổn định.
- Lưu Checkout Session (phiên mua hàng đang làm dở).
- Chọn địa chỉ, báo giá giao hàng, voucher và phương thức thanh toán.
- Checkout Preview: trả total dự kiến nhưng chưa tạo Order/Payment và chưa giữ tồn.
- Khi submit phải tải lại Cart, product/SKU, giá, tồn, địa chỉ, quote và voucher từ server.
- Tính tổng tại Order Service:

```text
total = subtotal - merchandise_discount
        + max(base_fee - shipping_discount, 0)
```

- Chống submit lặp bằng idempotency key.

#### Đơn hàng

- Tạo mã đơn duy nhất.
- Lưu Order, OrderItem và snapshot tiền/địa chỉ/hàng trong cùng transaction.
- Khách xem danh sách, chi tiết và hủy Order của chính mình.
- Người bán/quản trị viên xác nhận, đóng gói, giao và hoàn thành theo quyền.
- Lịch sử trạng thái và audit.

#### Saga và bù trừ

- Reserve inventory -> create Order -> create Payment.
- Payment success -> confirm Order -> commit inventory -> consume voucher.
- Payment failed/cancelled/expired -> cancel Order -> release inventory -> release voucher.
- Đây là compensation (giao dịch bù trừ), không phải rollback trực tiếp database của service khác.
- Event quan trọng được ghi vào outbox trong cùng transaction của Order.

#### Bảng chính

`checkout_sessions`, `checkout_session_items`, `orders`, `order_items`, `order_status_histories`, `idempotency_records`, `outbox_events`, `processed_events`.

### 4.7. Dịch vụ thanh toán — Payment Service

**Owner:** Người 4. **Database:** `payment_db`.

- Nhận `OrderPaymentContext` từ Order Service; frontend không tự gửi amount đáng tin.
- COD: Payment bắt đầu ở trạng thái chờ; chỉ được trả tiền sau khi thu COD theo policy.
- VNPay Sandbox: tạo URL có chữ ký.
- Return URL chỉ hiển thị UX; IPN là callback xác thực phía server.
- IPN kiểm tra chữ ký, merchant, order/payment reference, amount, trạng thái hiện tại và transaction code.
- Callback lặp chỉ được xử lý một lần.
- Payment attempt, lịch sử request/response đã lọc secret và audit đối soát.
- Hết hạn, retry payment, hủy payment theo policy.
- Hoàn tiền: P1.

#### Bảng chính

`payments`, `payment_transactions`, `payment_attempts`, `outbox_events`, `processed_events`.

### 4.8. Dịch vụ engagement và reporting — Engagement Service

**Owner:** Người 5. **Database:** `engagement_db`.

#### Review P0

- Khách chỉ được review OrderItem của mình khi Order đã hoàn thành.
- Một OrderItem chỉ tạo một review.
- Điểm từ 1 đến 5; comment phải được escape/sanitize trước hiển thị.
- Admin ẩn/hiện review.
- Phát event để Catalog cập nhật điểm trung bình và số review.

#### Wishlist và notification P1

- Thêm/xóa/xem sản phẩm yêu thích; một user/product không trùng.
- Thông báo trong hệ thống cho Order, Payment, Shipment.
- Email gửi sau khi transaction hoàn tất; có log delivery, retry và chống gửi trùng.

#### Báo cáo P0/P1

- Doanh thu cơ bản, số đơn theo trạng thái, best seller: P0.
- Theo category, theo ngày/tháng, refund/net revenue: P1.
- Không join trực tiếp `order_db`; tạo read model (bảng đọc tối ưu từ event).
- Chỉ tính Order theo trạng thái đã thống nhất, ví dụ `COMPLETED`.

#### Bảng chính

`reviews`, `favorites`, `notifications`, `notification_deliveries`, `revenue_daily`, `product_sales_summary`, `category_sales_summary`, `order_statistics`, `processed_events`, `outbox_events`.

## 5. Contract và luồng liên dịch vụ

### Contract đồng bộ tối thiểu

| Bên cung cấp | Bên dùng | Dữ liệu cần trả |
|---|---|---|
| Catalog | Cart, Order, Shipping | SKU/product ID, tên, giá, active, stock, trọng lượng, ảnh, attributes snapshot |
| Identity | Order, Shipping | user ID, role, address snapshot và ownership |
| Cart | Order | selected items gồm SKU/product ID và quantity |
| Promotion | Order | hợp lệ, discount hàng, discount ship, reservation ID, lý do từ chối |
| Shipping | Order | quote ID, provider, service, base fee, ETA, fingerprint, expires at |
| Order | Payment | order ID/code, user ID, amount, currency, payment method |
| Order | Engagement | review eligibility, OrderCompleted, OrderCancelled, OrderRefunded |

### Event tối thiểu

Mọi event nên có `eventId`, `eventType`, `eventVersion`, `producer`, `aggregateId`, `occurredAt`, `correlationId` và `payload`.

- `InventoryReserved`, `InventoryReleased`, `InventoryCommitted`.
- `OrderCreated`, `OrderConfirmed`, `OrderCancelled`, `OrderCompleted`, `OrderRefunded`.
- `PaymentSucceeded`, `PaymentFailed`, `PaymentRefunded`, `PaymentExpired`.
- `VoucherReserved`, `VoucherConsumed`, `VoucherReleased`.
- `ShipmentCreated`, `ShipmentDelivered`, `ShipmentFailed`.
- `ReviewCreated`, `ReviewHidden`.

## 6. Chức năng giao diện dự kiến

### Giao diện khách hàng

| Khu vực | Chức năng |
|---|---|
| Trang chủ | Category nổi bật, hàng mới, best seller, banner campaign nếu có |
| Danh sách sản phẩm | Tìm kiếm, lọc động, khoảng giá, sort, phân trang, empty state |
| Chi tiết sản phẩm | Ảnh, thuộc tính động, giá, tồn, review, thêm giỏ/mua ngay/yêu thích |
| Đăng nhập/đăng ký | Validation, lỗi rõ, loading, logout |
| Hồ sơ và địa chỉ | Sửa profile, sổ địa chỉ, đặt default, chọn mã hành chính |
| Giỏ hàng | Thay số lượng, xóa, chọn dòng mua, subtotal chỉ để tham khảo |
| Checkout | Địa chỉ, quote GHN, voucher, breakdown tiền, payment method, chống submit lặp |
| Payment result | Trạng thái redirect/đang chờ/thành công/thất bại, nút xem Order |
| Order | Danh sách, detail snapshot, timeline, cancel khi hợp lệ, tracking |
| Review | Danh sách review, tạo review từ OrderItem đủ điều kiện |
| Wishlist/notification | Danh sách yêu thích và thông báo; P1 |

### Giao diện quản trị/người bán

| Khu vực | Chức năng |
|---|---|
| Dashboard | Tổng quan Order, doanh thu, best seller, cảnh báo tồn thấp nếu làm |
| Catalog | Category tree, attribute, option, mapping category–attribute, product form động, ảnh |
| Kho | Tồn sẵn/đã giữ, lịch sử biến động, nhập/điều chỉnh kho, low stock |
| Promotion | Campaign/voucher CRUD, bật/tắt, rule, usage/reservation history |
| Order | Danh sách/lọc/detail, timeline, command chuyển trạng thái hợp lệ |
| Payment | Lọc/detail/audit, COD reconciliation theo policy |
| Shipping | Quote/shipment/detail/tracking/retry; P1 cho vận đơn thật |
| Review | Danh sách, lọc, ẩn/hiện review |
| Reporting | Revenue, best seller, category sales, net revenue; mở rộng P1 |

### Tiêu chuẩn giao diện

- Dùng Next.js, React và Tailwind CSS; dùng TypeScript (JavaScript có kiểm tra kiểu) nếu nhóm thống nhất từ đầu.
- Có trạng thái loading, lỗi, thành công, rỗng và disabled khi gửi form.
- Responsive cho mobile, tablet và desktop.
- Có label, validation message, focus dễ thấy và thao tác bàn phím cơ bản.
- Không tự tính total cuối hay tự xác nhận payment thành công ở frontend.

## 7. Công nghệ áp dụng

| Thành phần | Công nghệ | Lý do và phạm vi |
|---|---|---|
| Backend | Java 21, Spring Boot 3.x | Mỗi service là ứng dụng độc lập, có validation, security, REST API |
| Persistence | Spring Data JPA | Truy cập PostgreSQL theo entity/repository |
| Database | PostgreSQL | Hỗ trợ transaction, lock, JSONB cho thuộc tính động |
| Migration | Flyway | Phiên bản hóa schema; migration đã merge không sửa lại |
| Giao tiếp bất đồng bộ | RabbitMQ | Event-driven, retry, DLQ và outbox |
| API Gateway | Spring Cloud Gateway | Route, CORS, rate limiting, correlation ID, error normalization |
| Xác thực | Spring Security + JWT | Xác thực token và phân quyền server-side |
| API docs | OpenAPI/Swagger | Contract API, thử endpoint và bàn giao dễ hơn |
| Frontend | Next.js + React + Tailwind CSS | UI theo component, responsive, dễ tổ chức page/layout |
| Test backend | JUnit 5, Mockito, Spring Boot Test | Unit test, service/API/integration test |
| Test hạ tầng | Testcontainers | Chạy PostgreSQL/RabbitMQ thật trong test tự động |
| Test giao diện E2E | Playwright | Test luồng người dùng trên trình duyệt |
| Đóng gói | Docker, Docker Compose | Toàn nhóm chạy cùng môi trường |
| Cache/giới hạn request | Redis, nếu thật sự dùng | Cache hoặc rate limit; không bắt buộc P0 |
| Lưu ảnh | Cloudinary hoặc object storage | Chỉ thêm khi nhóm chốt; secret nằm trong biến môi trường |

## 8. Phân công cho 5 người và nhận xét

### Phân công đề xuất

| Người | Owner | P0 thực hiện | Giảm scope để bảo đảm tiến độ |
|---|---|---|---|
| 1 | Catalog + Inventory | Category, dynamic attribute, product, search/filter, reservation tồn kho | Không làm multi-warehouse và ProductVariant nâng cao trước |
| 2 | Identity + Cart | JWT, roles, profile/address, cart của user đăng nhập | Guest cart, social login và password reset nâng cao để P1 |
| 3 | Checkout + Order + Saga | Preview, create Order, snapshot, lifecycle, compensation, outbox | Buy-now, refund/return chi tiết để P1 |
| 4 | Payment + Shipping | COD, VNPay sandbox/IPN, GHN quote/fingerprint | Tạo vận đơn/tracking thật là P1 |
| 5 | Promotion + Engagement/Reporting | Voucher cơ bản/freeship/reservation, review, revenue và best seller | Campaign tài trợ, wishlist, notification, report mở rộng là P1 |

### Đánh giá tính chuẩn của cách phân công

Cách chia này **phù hợp với cách làm của lập trình viên chuyên nghiệp** ở các điểm sau:

- Chia theo domain nghiệp vụ và ownership rõ, thay vì chia một người chỉ làm database hoặc chỉ làm giao diện.
- Mỗi người có vertical slice: schema -> API -> giao diện -> test -> demo.
- Không có database dùng chung; giảm coupling (sự phụ thuộc chặt) giữa nhóm.
- Các phần có rủi ro cao như Inventory, Order, Payment đều có owner riêng.
- Contract và event là đầu ra bàn giao có thể kiểm thử, không chỉ truyền miệng.

Tuy nhiên, 8 service với 5 người là **đủ nhiều** cho đồ án 6–7 tuần. Không nên tiếp tục tách thêm service. Rủi ro lớn nhất không phải số dòng code mà là Docker, event, contract, migration, test và xử lý lỗi giữa service.

Các điều chỉnh bắt buộc để phân công không bị quá tải:

1. Người 5 chỉ hoàn thành voucher cơ bản + review + revenue/best seller ở P0; wishlist, notification, campaign phức tạp là P1.
2. Người 4 hoàn thành quote GHN trước; shipment/tracking/webhook thật chỉ bắt đầu sau khi COD và VNPay đã qua test.
3. Người 2 không nhận thêm frontend nền chung phức tạp; người này đang sở hữu hai service.
4. Người 3 phải được quyền từ chối contract mơ hồ vì Checkout là nơi các lỗi dữ liệu dồn về.
5. Mỗi service phải có OpenAPI, fake/mock contract và test riêng trước khi ghép E2E.

### Phần platform làm chung nhưng có người khởi tạo

| Hạng mục | Người khởi tạo | Người review |
|---|---|---|
| Quy ước PostgreSQL/Flyway/UUID/money | Người 1 + Người 3 | Cả nhóm |
| Gateway và JWT convention | Người 2 | Người 3 + Người 4 |
| Event envelope, Saga matrix, error code | Người 3 | Người 4 + Người 5 |
| RabbitMQ exchange, queue, retry, DLQ | Người 4 + Người 5 | Người 3 |
| Docker Compose và health check | Người 1 + Người 4 | Cả nhóm |
| OpenAPI convention | Người 2 + Người 3 | Cả nhóm |
| Contract/E2E test | Người 3 điều phối | Cả nhóm |

## 9. Ma trận kiểm thử bắt buộc trước demo

- Stock bằng 1, hai request reserve cùng lúc: chỉ một thành công.
- Cùng idempotency key checkout hai lần: chỉ có một Order.
- Voucher hết hạn/chưa bắt đầu/sai subtotal/hết quota: bị từ chối.
- Hai request dùng quota voucher cuối: chỉ một reservation thành công.
- Voucher ship không làm phí giao âm.
- Quote GHN hết hạn, sai user hoặc khác giỏ: bị từ chối.
- Payment callback sai signature/sai amount/sai reference: không được đánh dấu thành công.
- VNPay callback lặp: Payment và Order chỉ chuyển trạng thái một lần.
- Payment fail hoặc Order cancel: inventory/voucher được release đúng một lần.
- RabbitMQ down: database nghiệp vụ commit và outbox ở trạng thái chờ; broker phục hồi thì publish lại.
- Event `OrderCompleted` gửi lặp: reporting không cộng doanh thu/best seller hai lần.
- Khách A truy cập Order, Payment, Address, Review hoặc Quote của khách B: 403/404.
- Kiểm tra màn hình mobile, tablet, desktop cho Cart, Checkout, Payment Result, Order Detail và trang admin thay đổi.

## 10. Definition of Done cho một chức năng

Một chức năng chỉ được đánh dấu hoàn thành khi có đủ:

- Mô tả mục tiêu, P0/P1/P2 và owner.
- Migration từ database sạch, constraint/index phù hợp.
- API có validation, authentication và authorization.
- Business logic trong service; controller mỏng.
- Contract/API/event được cập nhật khi có dependency.
- UI có loading/error/empty/success và responsive.
- Test happy path, validation, ownership, money/stock/state/idempotency khi liên quan.
- Seed/factory và kịch bản demo.
- Không có secret trong Git/log; không còn TODO P0.
- PR được review và toàn bộ test/build cần thiết chạy thành công.

## 11. Quyết định cần nhóm khóa trước ngày code

1. P0 dùng Product đơn SKU hay ProductVariant.
2. Tất cả tiền dùng VND `BIGINT`.
3. Trạng thái chính xác của Order, Payment, Inventory Reservation và Shipment.
4. Chính sách COD: lúc nào Payment chuyển PAID.
5. Thời gian hết hạn của quote, voucher reservation và Payment.
6. Chính sách callback thanh toán đến trễ sau khi Order bị hủy.
7. Promotion có được cộng dồn voucher hàng và voucher freeship hay không.
8. API nội bộ dùng service token/HMAC nào.
9. Nhóm dùng mono-repo (một repository chứa nhiều service) hay nhiều repository; với đồ án nên ưu tiên mono-repo để giảm chi phí vận hành.
