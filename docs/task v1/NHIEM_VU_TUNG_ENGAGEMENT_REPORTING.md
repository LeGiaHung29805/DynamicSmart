# TÙNG — ĐÁNH GIÁ, TƯƠNG TÁC VÀ BÁO CÁO

Tùng sở hữu `engagement-service` và `engagement_db`. Tùng nhận fixture/contract: `ReviewEligibility`, `OrderCompleted`, ProductSummary và PaymentSucceeded; không truy vấn trực tiếp database của service khác.

## Chức năng được giao

### UC-15 — Đánh giá sản phẩm — P0

Khách hàng có thể:

- Xem đánh giá đang được hiển thị trên trang Product.
- Chấm từ 1 đến 5 sao và viết nhận xét cho dòng hàng đã mua.

Quy tắc nghiệp vụ:

- Chỉ được đánh giá khi Order thuộc chính khách hàng, Order `COMPLETED`, OrderItem thuộc đơn và chưa có đánh giá.
- Mỗi OrderItem chỉ được đánh giá một lần.
- Tùng gọi `ReviewEligibility` của Hiếu; không tự truy vấn Order DB hoặc tự viết lại logic điều kiện.
- Quản trị viên được ẩn đánh giá vi phạm và bắt buộc lưu lý do. Không xóa cứng đánh giá đã có audit.
- Nội dung đánh giá là dữ liệu không tin cậy, phải làm sạch trước khi hiển thị.

### UC-18 — Doanh thu và sản phẩm bán chạy — P0

Quản trị viên có thể:

- Xem doanh thu cơ bản theo ngày đã chốt.
- Xem số Order hoàn tất.
- Xem Product/Variant bán chạy.

Quy tắc nghiệp vụ:

- Chỉ cập nhật từ `OrderCompleted`; PaymentSucceeded chỉ dùng thông báo/đối soát, không cộng doanh thu.
- Event lặp không được cộng doanh thu hoặc số lượng bán hai lần.
- Doanh thu P0 là tổng `final_total` của Order hoàn tất, không gọi là lợi nhuận và chưa trừ hoàn tiền.
- Tiến nhận contract bán chạy để hiển thị tại Catalog; Tùng không ghi vào Catalog DB.

### UC-24 — Danh sách yêu thích — P1

- Khách thêm/xóa Product yêu thích và xem danh sách của mình.
- Một Product chỉ xuất hiện một lần trong danh sách của một khách.
- Không đọc/sửa danh sách của khách hàng khác.

### Hỏi đáp sản phẩm và thông báo — P1

Hỏi đáp sản phẩm:

- Khách gửi câu hỏi cho Product.
- Quản trị viên trả lời, ẩn/hiện nội dung vi phạm.
- Không có câu trả lời của seller vì DynamicMart chỉ có một doanh nghiệp bán hàng.

Thông báo trong hệ thống:

- Hiển thị thông báo về Order/thanh toán khi có event hợp lệ.
- Xem danh sách và đánh dấu đã đọc.
- Event/retry lặp không tạo thông báo trùng.

### Trò chuyện Customer–Admin — P2

- Khách tạo hội thoại hỗ trợ; quản trị viên nhận, gán người xử lý, trả lời và đóng hội thoại.
- Chỉ customer sở hữu conversation được đọc/gửi tin; chỉ `ADMIN` được xử lý inbox.
- Mỗi message có người gửi, thời gian, idempotency key và audit.
- Không có chat với seller; P2 không được chặn demo P0/P1.

## Giao diện cần làm

### Giao diện khách hàng

- Danh sách đánh giá, form tạo đánh giá từ OrderItem đủ điều kiện.
- Danh sách yêu thích P1, hỏi đáp P1, notification list P1.
- Trang hội thoại hỗ trợ P2 với mock trước khi nối thật.

### Giao diện quản trị

- Danh sách/ẩn đánh giá và lý do moderation.
- Dashboard doanh thu, Product/Variant bán chạy.
- Quản trị hỏi đáp P1, inbox trò chuyện P2.

## Phần làm ngay và phần cần chờ

| Làm ngay | Chờ để nối thật |
|---|---|
| Review/report UI với fixture, read model, processed_events, mock Q&A/notification/chat | Hiếu: ReviewEligibility/OrderCompleted; Tiến: ProductSummary; Hưng: PaymentSucceeded |

## Dữ liệu bàn giao

- Nhận từ Hiếu: ReviewEligibility, OrderCompleted, OrderCancelled.
- Nhận từ Tiến: ProductSummary để hiển thị Product/đánh giá và gửi rating aggregate nếu có.
- Nhận từ Hưng: PaymentSucceeded cho notification/audit, không dùng tính Revenue.
- Giao cho Tiến: ReviewCreated/ReviewHidden khi Catalog cần cập nhật điểm đánh giá.

## Kiểm thử bắt buộc

- Không mua/chưa hoàn tất/khác owner/đánh giá trùng đều bị từ chối.
- Ẩn đánh giá có lý do và không làm mất audit.
- `OrderCompleted` lặp không cộng Revenue/Best Seller hai lần.
- Wishlist và Q&A chặn truy cập chéo customer.
- Notification/message retry không duplicate.
- Conversation chỉ đúng customer/admin có quyền truy cập.

## Ràng buộc dữ liệu và tích hợp cần thực hiện

- Điểm đánh giá là bắt buộc, từ 1 đến 5; một dòng hàng trong đơn chỉ có một đánh giá.
- Các trạng thái đánh giá, hỏi đáp, thông báo và trò chuyện phải được kiểm tra theo danh sách giá trị cho phép.
- Báo cáo chỉ cập nhật từ sự kiện đơn hoàn tất đã được kiểm tra không trùng; sự kiện gửi lại không được làm tăng doanh thu hay số lượng bán.
