# CHUYỂN GIAO VAI TRÒ TỪ FASHION ECOMMERCE SANG DYNAMICMART

## 1. Mục đích

Phân công này giữ bốn thành viên của Fashion Ecommerce ở domain gần nhất với nghiệp vụ họ đã làm, để tận dụng hiểu biết về tiền, hàng, quyền truy cập và trạng thái. DynamicMart có thêm domain Engagement & Reporting, vì vậy cần một thành viên thứ năm; không giao chồng domain này lại cho Thảo hoặc Hiếu.

Tài liệu này chỉ phân công ownership cho DynamicMart. Nó không mang kiến trúc Laravel, MySQL hay Blade của dự án cũ sang dự án mới. DynamicMart dùng service ownership, REST/event contract, PostgreSQL, RabbitMQ và outbox theo `00_OVERVIEW.md` và `06_PHAN_LAM_CHUNG.md`.

## 2. Mapping owner

| Thành viên cũ | Ownership ở Fashion Ecommerce | Vai trò DynamicMart | Lý do và phạm vi giữ lại |
|---|---|---|---|
| Tiến | Catalog, Product, ProductVariant, Inventory | Người 1 — Catalog & Inventory | Tiếp tục sở hữu dữ liệu bán được: category, SKU, giá, tồn, lọc/tìm kiếm và kiểm soát đồng thời. Dynamic attribute thay thế phần thuộc tính Fashion/Watch cố định; ProductVariant P0/P1 phải được nhóm chốt trước khi Cart và Checkout code. |
| Thảo | Identity, Customer, Address, Cart, Voucher, Review, Wishlist, Chat | Người 2 — Customer Identity, Cart & Promotion | Giữ Identity, Address, Cart và toàn bộ Voucher lifecycle vì chúng vẫn gắn trực tiếp với customer/cart/checkout. Review và Wishlist chuyển ownership sang Người 5; Chat không thuộc phạm vi P0/P1 hiện tại. |
| Hiếu | Checkout, Order, Lifecycle, Dashboard, Reporting | Người 3 — Checkout & Order | Giữ checkout orchestration, snapshot, idempotency, state machine và compensation. Reporting chuyển ownership sang Người 5; Hiếu là nguồn chuẩn của event Order và reviewer cho định nghĩa doanh thu. |
| Hưng | Payment, Shipping, Notification | Người 4 — Payment & Shipping | Giữ COD, VNPay, callback/IPN, idempotency, audit và Shipping P1. Notification chuyển ownership sản phẩm sang Người 5, còn Hưng bàn giao quy tắc delivery/retry và event Payment/Shipment. |
| Thành viên thứ 5 — cần chốt tên | Chưa có owner tương đương một-một | Người 5 — Engagement & Reporting | Sở hữu Review, Wishlist, Notification P1, read model, Revenue và Best Seller. Nhận kiến thức nghiệp vụ từ Thảo, Hiếu, Hưng và Tiến; không sửa database/service của họ trực tiếp. |

## 3. Bàn giao bắt buộc cho Người 5

| Nguồn | Nội dung bàn giao | Contract/ranh giới DynamicMart |
|---|---|---|
| Thảo → Người 5 | Điều kiện review, chống review trùng, ownership khách hàng; hành vi Wishlist | Người 5 gọi `ReviewEligibility` của Order Service và dùng user ID đã xác thực; không truy vấn trực tiếp Identity/Order database. |
| Hiếu → Người 5 | Ý nghĩa `OrderCompleted`, `OrderCancelled`, `OrderRefunded`; snapshot tiền hàng và quy tắc doanh thu | Người 3 phát Order event. Người 5 chỉ cập nhật read model từ event idempotent; không tự tính tổng tiền khác với Order. |
| Hưng → Người 5 | Side effect sau commit, chống gửi notification trùng, retry và lọc dữ liệu nhạy cảm | `PaymentSucceeded` chỉ phục vụ notification/đối soát; Revenue và Best Seller chỉ dựa trên `OrderCompleted`. |
| Tiến → Người 5 | `ProductSummary`, liên kết product ID và cách hiển thị rating | Người 5 phát `ReviewCreated`/`ReviewHidden`; Người 1 cập nhật rating/catalog theo contract, không để Engagement ghi `catalog_db`. |

## 4. Khác biệt phải nhớ khi chuyển dự án

- Dự án cũ là Laravel monolith, dùng transaction và foreign key chung. DynamicMart không truy vấn chéo database; liên module đi qua REST contract hoặc RabbitMQ event.
- `ProductVariant` là trung tâm của dự án cũ. DynamicMart phải khóa quyết định P0 dùng Product đơn SKU hay ProductVariant trước khi triển khai Cart/Checkout; mọi contract dùng cùng một saleable ID.
- Checkout cũ điều phối trong một transaction. DynamicMart dùng Saga: reserve inventory/voucher, tạo Order/Payment và compensation bằng event/outbox; không rollback database service khác trực tiếp.
- `PaymentSucceeded` không phải bằng chứng ghi doanh thu. Người 5 chỉ cập nhật Revenue/Best Seller khi nhận `OrderCompleted` đúng một lần.
- Address thuộc Người 2, quote/shipping thuộc Người 4, Order snapshot thuộc Người 3. Người 3 chỉ điều phối qua contract, không lấy ownership các module này.

## 5. Cách làm việc và review chéo

- Tiến review contract saleable product, price/stock snapshot và reservation cạnh tranh trong Cart/Checkout.
- Thảo review identity, address ownership, cart ownership và Voucher reservation/stacking.
- Hiếu review total server-side, Order transition, Saga/compensation và semantics của event cho Reporting.
- Hưng review callback/IPN, amount/signature/idempotency, timeout provider và event Payment/Shipment.
- Người 5 review idempotent consumer, `processed_events`, review eligibility và quy tắc read model; đồng thời yêu cầu Thảo/Hiếu/Hưng review lần đầu với phần nghiệp vụ được bàn giao.

## 6. Điều kiện chốt phân công

Trước khi code, nhóm cần điền tên Người 5 và tổ chức một buổi handoff ngắn. Handoff phải chốt một fixture cho mỗi contract: `PurchasableProduct`, `CheckoutCartItem`, `AddressSnapshot`, `VoucherReservationResult`, `OrderPaymentContext`, `OrderCompleted` và `ReviewEligibility`. Mỗi fixture phải có trường hợp hợp lệ, hết hạn hoặc xử lý lặp tương ứng.
