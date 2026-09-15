# HIẾU — XÁC NHẬN MUA, ĐƠN HÀNG VÀ ĐIỀU PHỐI GIAO DỊCH

Hiếu sở hữu `order-service` và `order_db`; là nguồn chuẩn của `OrderCompleted` và dữ liệu Order.

## Chức năng được giao

### UC-09 — Checkout từ Giỏ hàng hoặc Mua ngay — P0

Khách hàng đã đăng nhập có thể:

- Chọn các dòng cần mua trong Giỏ hàng hoặc đi từ Mua ngay một Variant/số lượng.
- Chọn địa chỉ đã lưu, thêm địa chỉ mới ngay tại Checkout và tự chọn địa chỉ đó.
- Chọn mã giảm giá, phương thức/thời điểm thanh toán.
- Xem trước giá niêm yết, direct sale, voucher giảm giá, phí GHN, giảm phí ship và tổng cuối.
- Hủy bản nháp Checkout trước khi có Order.

Quy tắc nghiệp vụ:

- `CART` và `BUY_NOW` dùng chung Checkout Session; Buy Now chỉ có đúng một dòng hàng và không sửa Giỏ.
- Việc chọn hoặc thêm/lưu địa chỉ do thành phần địa chỉ của Thảo thực hiện. Hiếu chỉ dùng `addressId` đã hợp lệ để xin báo giá GHN; không báo giá khi chưa có địa chỉ.
- Guest Mua ngay được Login/Register xong quay về đúng Variant/số lượng bằng selection ký ngắn hạn; sau login phải kiểm tra lại Product/Variant.
- Checkout Session chỉ là bản nháp 15 phút. Mở Preview, hủy hoặc hết hạn không giữ tồn/mã giảm giá.
- Mọi giá niêm yết, direct sale, tồn, Product, địa chỉ, voucher và phí ship được kiểm tra lại ở máy chủ khi tạo Order.
- Quote GHN chỉ dùng khi chưa hết hạn/chưa dùng và fingerprint khớp customer, địa chỉ, hàng/số lượng/cân nặng, voucher ship, dịch vụ và kho gửi.

### UC-10 — Tạo Order và lưu bản chụp — P0

Khi khách bấm Tạo Order:

1. Kiểm tra Idempotency-Key, Checkout Session và quyền sở hữu.
2. Lấy lại Cart hoặc Buy Now selection từ server.
3. Kiểm tra địa chỉ từ Thảo, Product/tồn từ Tiến, direct sale/voucher từ Thảo, quote từ Hưng.
4. Bắt đầu Saga: giữ voucher, giữ tồn, tạo Order và snapshot.
5. Tạo Payment khi tổng tiền lớn hơn 0.
6. Chốt/hoàn tồn và voucher theo kết quả thanh toán.

Order phải lưu bất biến: Product/Variant/SKU/ảnh/giá niêm yết/direct sale/giá sau sale/số lượng, Address, voucher, phí GHN/dịch vụ/ETA, tổng tiền và cách thanh toán. Sửa Product/direct sale/địa chỉ/voucher về sau không đổi đơn cũ.

### UC-13 và UC-14 — Lịch sử, chi tiết và vòng đời đơn — P0

Khách hàng có thể xem danh sách, chi tiết, timeline và xác nhận **Đã nhận hàng** cho đơn của chính mình.

Quản trị viên chỉ được đưa Order qua `CONFIRMED → PACKING → SHIPPING`; với đơn trả sau mới được chuyển tiếp sang `HANDOVER_PENDING`. Quản trị viên không được tự đặt `DELIVERED` hoặc `COMPLETED`.

Quy tắc nghiệp vụ:

- Khách không hủy Order sau khi Order đã được tạo.
- Trả trước VNPay: `PENDING_PAYMENT → CONFIRMED → PACKING → SHIPPING`; khi khách xác nhận đã nhận hàng, ghi `DELIVERED` rồi hoàn tất. Thanh toán thất bại/hết hạn chỉ được hủy/trả reservation đúng một lần khi Order còn `PENDING_PAYMENT`.
- Trả sau COD/VNPay: `CONFIRMED → PACKING → SHIPPING → HANDOVER_PENDING`. `PaymentSucceeded` và customer xác nhận đã nhận hàng có thể đến trước/sau; khi đủ cả hai, ghi `DELIVERED` và hoàn tất đúng một lần.
- Đơn 0 đồng: `CONFIRMED → PACKING → SHIPPING`; customer xác nhận đã nhận hàng thì ghi `DELIVERED` và hoàn tất. Không tạo Payment bên ngoài.
- `PaymentDue` do Hiếu phát khi Order `POSTPAID + VNPAY` chuyển `HANDOVER_PENDING`; Hưng nhận event này để tạo URL/QR. COD không dùng event này.
- Không để admin tự ghi “đã giao”, hủy Order sau `CONFIRMED`, hoặc hoàn tất thay customer trong phạm vi P0.

### UC-17 — Dọn Giỏ hàng sau mua — P0

- Chỉ phát `OrderConfirmed` source `CART` sau khi đơn đã được xác nhận theo rule thanh toán.
- Gửi CartItem id/version/quantity snapshot cho Thảo dọn một lần.
- Đơn trả trước chỉ dọn sau PaymentSucceeded; fail/hết hạn phải giữ Giỏ để khách mua lại.
- Buy Now không bao giờ tạo yêu cầu dọn Giỏ.

## Giao diện cần làm

### Giao diện khách hàng

- Checkout: chọn/đổi/thêm địa chỉ, hiển thị direct sale, chọn voucher, phí GHN, breakdown tiền, payment choice, lỗi quote và retry.
- Nút Tạo Order có loading/chống bấm lặp và thông báo lỗi nghiệp vụ rõ.
- Danh sách/chi tiết/timeline Order, trạng thái thanh toán riêng, nút Đã nhận hàng đúng điều kiện.

### Giao diện quản trị

- Danh sách, tìm kiếm/lọc, chi tiết Order.
- Snapshot hàng/địa chỉ/voucher/phí ship, lịch sử trạng thái và command hợp lệ.

## Phần làm ngay và phần cần chờ

| Làm ngay | Chờ để nối thật |
|---|---|
| Màn Checkout/Order bằng fixture, state machine, snapshot và validation giao diện | Tiến: tồn; Thảo: Giỏ/địa chỉ/voucher; Hưng: quote GHN và Payment event |

## Dữ liệu bàn giao

- Nhận từ Tiến: kiểm tra Product và giữ/chốt/trả tồn.
- Nhận từ Thảo: dòng Giỏ, AddressSnapshot, kiểm tra/giữ/dùng/trả voucher.
- Nhận từ Hưng: quote GHN, OrderPaymentContext result, PaymentSucceeded/Failed/Expired/Due.
- Giao cho Thảo: OrderConfirmed để dọn Giỏ.
- Giao cho Tùng: ReviewEligibility, OrderCompleted, OrderCancelled.

## Kiểm thử bắt buộc

- Cart trống, địa chỉ sai quyền, Product ngừng bán, hết tồn, voucher sai và quote sai fingerprint bị từ chối.
- Cùng Idempotency-Key chỉ tạo một Order và một side effect.
- Snapshot không đổi sau khi dữ liệu nguồn bị sửa.
- Hủy/hết hạn bản nháp không giữ/trả nhầm tồn hoặc voucher.
- Saga lỗi/prepaid fail/hết hạn trả tồn/voucher đúng một lần; callback đến muộn không mở lại Order.
- Dọn Giỏ chỉ sau OrderConfirmed và không xóa dòng đã sửa.
- Customer A không đọc/xác nhận đơn của Customer B.

## Ràng buộc dữ liệu và tích hợp cần thực hiện

- Phiên đặt hàng chỉ chuyển từ đang hoạt động sang đã hủy nếu chưa có đơn. Nếu yêu cầu hủy trùng lúc chuỗi tạo đơn đang chạy, chuỗi đó phải trả tồn và lượt mã giảm giá đúng một lần trước khi đóng phiên.
- Trước khi tạo đơn, phải kiểm tra công thức: tổng cuối = tiền hàng - giảm theo hàng - giảm toàn đơn + phí giao - giảm phí giao. Các số tiền không âm và tiền giảm phân bổ cho từng dòng hàng phải khớp tổng của đơn.
- Bản chụp phí giao hàng trong đơn phải lưu mã báo giá GHN đã dùng, dấu vết kiểm tra đầu vào và kích thước kiện hàng; nhờ đó có thể kiểm tra lại đúng báo giá đã dùng.
- Mã giảm giá trong phiên xác nhận chỉ là lựa chọn xem trước; chỉ lưu mã giữ lượt sau khi Thảo giữ thành công trong chuỗi tạo đơn.
