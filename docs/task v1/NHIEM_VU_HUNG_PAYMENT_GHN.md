# HƯNG — THANH TOÁN VÀ PHÍ GIAO HÀNG GHN

Hưng sở hữu `payment-service` và `payment_db`. Hưng không ghi trực tiếp Order DB, không dọn Giỏ hàng và không tạo vận đơn/tracking GHN.

## Chức năng được giao

### UC-11 — Thanh toán COD — P0

- Khi Order dùng COD, tạo Payment `PENDING`, không đánh dấu đã trả tiền từ đầu.
- Chỉ ghi `PAID` khi có xác nhận thu tiền từ luồng vận hành được bảo vệ.
- Payment COD phải khớp đúng Order, customer và tổng tiền snapshot.

### UC-12 — Thanh toán VNPay — P0

Khách hàng có thể thanh toán bằng VNPay theo hai thời điểm:

- `PREPAID + VNPAY`: tạo URL ngay sau Order, thanh toán trước fulfillment.
- `POSTPAID + VNPAY`: tạo URL/QR khi đơn ở Handover Pending.

Quy tắc nghiệp vụ:

- URL/QR chứa amount/reference do server tạo, TTL 15 phút và chữ ký hợp lệ.
- Return URL trên trình duyệt không phải bằng chứng thanh toán; chỉ IPN đã kiểm tra chữ ký, reference, amount và trạng thái mới được cập nhật Payment.
- Callback lặp chỉ tạo một PaymentSucceeded; callback muộn sau prepaid expiry chỉ audit.
- Prepaid hết hạn/fail phát event để Hiếu hủy/release đúng một lần.
- Postpaid QR hết hạn hoặc thất bại chỉ kết thúc lần tạo QR đó; Payment của cả đơn vẫn `PENDING` để tạo QR mới khi Order còn Handover Pending, không hủy Order và không phát `PaymentFailed`/`PaymentExpired` cho Order.
- Từ chối `PREPAID + COD`; đơn 0 đồng dùng `NOT_REQUIRED + FREE`, không tạo Payment/URL ngoài.

### UC-21 — Danh mục địa chỉ GHN và báo giá phí giao hàng — P0

Hưng phải triển khai thật, không chỉ viết tài liệu:

- Đồng bộ/cache danh mục Tỉnh/Thành phố và Phường/Xã từ GHN.
- Cung cấp API qua cổng API của hệ thống để thành phần địa chỉ do Thảo làm lấy danh sách tỉnh và phường/xã theo tỉnh. Hưng cung cấp dữ liệu/cấu trúc API, không làm giao diện chọn địa chỉ.
- Cung cấp API nội bộ để Thảo kiểm tra Ward thuộc đúng Province trước khi lưu Address.
- Báo giá GHN phía server từ địa chỉ canonical, cân nặng/kích thước, hàng mua và kho gửi cố định.

Quote trả về mã quote, phí, dịch vụ, ETA và thời hạn. Mỗi quote có fingerprint server-side gồm customer, địa chỉ canonical, Variant/số lượng/cân nặng, voucher ship, dịch vụ và kho gửi.

Quy tắc nghiệp vụ:

- Không nhận phí ship, mã GHN, token, Shop ID hoặc địa chỉ kho đáng tin từ frontend.
- Đổi địa chỉ/hàng/số lượng/voucher ship/dịch vụ/kho gửi phải quote lại.
- Quote hết hạn, đã dùng, sai customer hoặc fingerprint khác bị từ chối.
- GHN timeout/lỗi/địa chỉ không hỗ trợ trả lỗi retry rõ ràng; không tự dùng phí 0 hoặc bảng phí giả.
- P0 chỉ tính phí; không tạo vận đơn, tracking hoặc webhook GHN.

### UC-22 — Audit, outbox và xử lý lặp — P0

- Lưu Payment Attempt, callback audit, trạng thái trước/sau và dữ liệu đã lọc secret.
- Payment event phải đi qua Outbox; RabbitMQ lỗi không làm mất Payment đã commit.
- Idempotency key/reference/provider transaction không được gắn cho nhiều Order.
- Hưng nhận `PaymentDue` do Hiếu phát khi Order `POSTPAID + VNPAY` vào `HANDOVER_PENDING`; COD không dùng event này. Chỉ `PaymentSucceeded` do Hưng phát khi Payment của cả đơn chuyển `PAID`. `PaymentFailed` và `PaymentExpired` chỉ áp dụng cho Payment trả trước.

## Giao diện cần làm

### Giao diện khách hàng

- Chọn cách và thời điểm thanh toán chỉ trong các tổ hợp backend cho phép.
- Trang VNPay return: đang chờ/thành công/thất bại/hết hạn, nút xem Order hoặc thử lại khi rule cho phép.
- Checkout hiển thị phí GHN/ETA từ quote server, loading/error/retry rõ ràng.

### Giao diện quản trị

- Xem Payment, Payment Attempt, callback audit đã lọc dữ liệu nhạy cảm.
- Command xác nhận thu COD theo quyền.

## Phần làm ngay và phần cần chờ

| Làm ngay | Chờ để nối thật |
|---|---|
| Payment UI bằng fixture, client GHN/VNPay mock, migration cache địa giới và contract test | Hiếu: OrderPaymentContext/Order state; credential GHN/VNPay ở môi trường deploy |

## Dữ liệu bàn giao

- Cho Thảo/frontend: danh mục Province/Ward và kết quả kiểm tra địa giới.
- Cho Hiếu: Quote hợp lệ, `PaymentSucceeded`; với Payment trả trước mới có thêm `PaymentFailed` hoặc `PaymentExpired`.
- Nhận từ Hiếu: `PaymentDue` khi Order `POSTPAID + VNPAY` vào `HANDOVER_PENDING` để tạo URL/QR VNPay trả sau.
- Cho Tùng: PaymentSucceeded chỉ phục vụ thông báo/audit nếu cần, không tính doanh thu.

## Kiểm thử bắt buộc

- Province/Ward sai quan hệ, inactive hoặc client tự bịa bị từ chối.
- GHN timeout/quote hết hạn/quote fingerprint sai không tạo Order.
- Chữ ký, amount, reference sai; callback lặp/đến muộn đều an toàn.
- COD không `PAID` sớm; prepaid fail/expiry chỉ phát release một lần.
- Postpaid hết hạn tạo attempt mới đúng rule, không hủy Order.
- Không log token, chữ ký, secret hay URL thanh toán nhạy cảm.

## Ràng buộc dữ liệu và tích hợp cần thực hiện

- Lần xác nhận thu COD thành công phải lưu số tiền thực thu, mã biên nhận, quản trị viên xác nhận và thời điểm xác nhận; số tiền phải đúng bằng số tiền cần thu.
- Trạng thái thanh toán, thời điểm thanh toán và cách thanh toán phải thuộc các giá trị cho phép; giao diện không được sửa trực tiếp số tiền hay trường lịch sử.
- Báo giá GHN phải kiểm tra phí không âm, cân nặng dương và kích thước hợp lệ. Khi tạo đơn, Hiếu lưu lại mã báo giá, dấu vết kiểm tra, kích thước kiện, dịch vụ và thời gian dự kiến.
- Kiểm thử thêm: COD thiếu người xác nhận/biên nhận/sai số tiền bị từ chối; thông báo thanh toán COD trùng chỉ tạo một kết quả thành công.
