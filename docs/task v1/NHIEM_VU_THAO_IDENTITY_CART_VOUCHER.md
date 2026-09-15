# THẢO — TÀI KHOẢN, ĐỊA CHỈ, GIỎ HÀNG VÀ MÃ GIẢM GIÁ

Thảo sở hữu Profile/Address/Admin User Management trong `identity-service`, `identity_db`, cùng `cart-service` và `cart_db`. Backend đăng ký, đăng nhập, quên mật khẩu và phát token thuộc phần nền chung do trưởng nhóm quản lý; Tiến làm giao diện Auth.

## Chức năng được giao

### UC-03 — Hồ sơ cá nhân — P0

Khách hàng xem và sửa hồ sơ của chính mình. Thảo dùng JWT do phần nền chung phát để xác định customer, không tự triển khai đăng ký, đăng nhập, quên mật khẩu, đổi mật khẩu hoặc Refresh Token.

### UC-03A — Sổ địa chỉ và chọn địa chỉ giao hàng — P0

Khách hàng có thể:

- Xem, thêm, sửa, ngừng dùng và đặt một địa chỉ mặc định.
- Chọn địa chỉ đã lưu ở bước Checkout.
- Thêm địa chỉ ngay trong Checkout; lưu xong địa chỉ được chọn ngay và còn lại cho lần mua sau.

Mỗi địa chỉ gồm người nhận, số điện thoại, số nhà/đường, Tỉnh/Thành phố và Phường/Xã.

Quy tắc nghiệp vụ:

- Khách chỉ nhập số nhà/đường. Thảo gọi API địa giới GHN qua cổng API của hệ thống để lấy danh sách Tỉnh/Thành phố, rồi gọi tiếp API lấy Phường/Xã theo Tỉnh/Thành phố đã chọn; khách không được tự nhập mã hoặc tên địa giới.
- Thảo phụ trách toàn bộ thành phần chọn và lưu địa chỉ, kể cả khi nó được hiển thị ngay trong bước xác nhận đặt hàng. Hưng cung cấp dữ liệu/cấu trúc API địa giới và kiểm tra địa chỉ ở máy chủ; trình duyệt không gọi trực tiếp dịch vụ GHN để không lộ cấu hình.
- Máy chủ kiểm tra Ward thuộc đúng Province trước khi lưu. Không tin mã/tên địa giới do trình duyệt tự sửa.
- Một khách chỉ có một địa chỉ mặc định đang hoạt động.
- Xóa địa chỉ là chuyển `INACTIVE`, không xóa cứng; địa chỉ cũ trong Order snapshot không thay đổi.

Trình tự bắt buộc:

1. Thảo hiển thị và lưu địa chỉ hợp lệ của khách, hoặc dùng địa chỉ đã lưu.
2. Hiếu chỉ nhận `addressId` sau khi địa chỉ đã được chọn/lưu; sau đó mới yêu cầu Hưng báo giá GHN.
3. Đổi địa chỉ làm báo giá cũ mất hiệu lực và phải báo giá lại; Thảo không tự tính hoặc gửi phí giao hàng.

### UC-08 — Giỏ hàng — P0

Khách hàng có thể:

- Xem giỏ hàng của mình.
- Thêm Variant, sửa số lượng, xóa dòng hàng và chọn/bỏ chọn dòng mua.
- Xem cảnh báo Product/Variant ngừng bán hoặc số lượng không còn hợp lệ.

Quy tắc nghiệp vụ:

- Một Variant chỉ có một dòng trong một giỏ; thêm lại thì tăng số lượng theo rule tồn kho.
- Giỏ không lưu giá đáng tin cậy; Checkout luôn lấy lại giá/trạng thái/tồn từ Tiến.
- Mỗi CartItem có `version`; khi khách sửa quantity hoặc chọn/bỏ chọn phải tăng version.
- Sau OrderConfirmed, chỉ xóa đúng những dòng đã mua có `id`, quantity và version trùng snapshot. Buy Now, dòng không chọn, dòng đã sửa hoặc đơn trả trước thất bại không bị xóa.
- Guest Cart và gộp giỏ sau đăng nhập là P1, không chặn P0.

### UC-19 và UC-20 — Giảm giá trực tiếp, mã giảm giá/khuyến mãi — P0

Quản trị viên có thể tạo, sửa, tắt direct sale và mã giảm giá; khách hàng đã đăng nhập xem/chọn mã hợp lệ.

Direct sale:

- Admin chọn một hoặc nhiều Variant, cấu hình giảm cố định/phần trăm, thời gian và trạng thái campaign.
- Giá niêm yết vẫn do Tiến quản lý trong Catalog; Thảo không sửa giá gốc mà chỉ trả giá sale tính từ rule còn hiệu lực.
- Một Variant chỉ có một direct sale hiệu lực tại một thời điểm P0.
- Cung cấp API internal/public contract để Tiến hiện giá gốc gạch, giá sale, badge phần trăm và thời hạn.

Hỗ trợ tối thiểu:

- Giảm toàn đơn, giảm phí giao hàng, giảm Product/danh mục/danh sách Product.
- Giảm tiền cố định hoặc phần trăm có mức giảm tối đa.
- Điều kiện thời gian, tổng đơn tối thiểu, tổng hàng đủ điều kiện tối thiểu, tổng lượt dùng và lượt dùng mỗi khách.
- Tối đa một mã giảm hàng hóa và một mã giảm phí giao hàng trong P0.
- Voucher có `DEFAULT_FOR_ELIGIBLE` (mọi customer đã login và đủ điều kiện), `ASSIGNED_ONLY` (cấp riêng vào ví của từng account) hoặc `CODE_ONLY` (nhập code). Admin có thể cấp/revoke voucher `ASSIGNED_ONLY` cho một account; phải lưu audit.
- Voucher hàng hóa tính trên giá sau direct sale. Product Card chỉ hiện gợi ý “Có voucher” nếu server cho phép; không gạch giá theo voucher.

Quy tắc nghiệp vụ:

- Trình duyệt không tự tính hoặc quyết định số tiền giảm.
- Giỏ/Checkout Preview chỉ xem trước. Khi khách bấm Tạo Order, Order Service của Hiếu kiểm tra lại rồi mới gọi giữ lượt mã.
- Đơn trả trước giữ lượt mã đến khi thanh toán thành công/thất bại/hết hạn. Đơn trả sau/0 đồng dùng mã khi OrderConfirmed.
- Giữ, dùng và trả lượt mã phải idempotent; không được làm tổng tiền âm.

### Quản lý người dùng — P0

Quản trị viên có thể:

- Tìm kiếm/lọc danh sách khách hàng và quản trị viên.
- Xem hồ sơ cơ bản, trạng thái, vai trò, đăng nhập gần nhất và lịch sử thao tác.
- Khóa, mở khóa, vô hiệu hóa hoặc đổi vai trò có lý do.

Quy tắc nghiệp vụ:

- Không hiển thị password hash, Refresh Token hoặc token thô.
- Không tự đổi vai trò/trạng thái của chính mình và không được vô hiệu hóa quản trị viên đang hoạt động cuối cùng.
- Mọi thay đổi có idempotency key, lý do và audit bất biến.

## Giao diện cần làm

### Giao diện khách hàng

- Đăng ký, đăng nhập, đăng xuất, hồ sơ.
- Sổ địa chỉ, chọn địa chỉ mặc định, form thêm/sửa dùng danh sách GHN.
- Giỏ hàng: chọn dòng, quantity, xóa, trạng thái hết hàng và cảnh báo giá chỉ là tạm tính.
- Ví mã giảm giá, danh sách mã đủ/không đủ điều kiện và lý do; phân biệt voucher mặc định, voucher được cấp riêng và voucher nhập mã.

### Giao diện quản trị

- Danh sách người dùng, tìm kiếm/lọc, chi tiết, khóa/mở khóa/vô hiệu hóa/đổi vai trò và audit.
- Danh sách/form direct sale theo Variant, lịch campaign và trạng thái; danh sách/form mã giảm giá, cách phân phối, điều kiện, phạm vi áp dụng và lịch sử cấp/giữ/dùng/trả lượt.

## Phần làm ngay và phần cần chờ

| Làm ngay | Chờ để nối thật |
|---|---|
| Auth/Profile/Cart/Direct Sale/Voucher UI bằng dữ liệu mẫu, Address form và Admin User UI | Hưng: danh mục GHN; Tiến: Variant mua được/giá niêm yết; Hiếu: Checkout, OrderConfirmed và contract reserve Voucher |

## Dữ liệu bàn giao

- Cho Tiến: DirectSalePrice theo Variant.
- Cho Hiếu: AddressSnapshot, CheckoutCartItem, DirectSale snapshot, VoucherValidation/Reservation/Discount.
- Nhận từ Hưng: Province, Ward, kết quả kiểm tra địa giới.
- Nhận từ Tiến: thông tin Variant có thể bán.
- Nhận từ Hiếu: OrderConfirmed để dọn Cart một lần.

## Kiểm thử bắt buộc

- Đăng nhập sai, token hết hạn, role escalation và truy cập chéo dữ liệu bị chặn.
- Ward không thuộc Province bị từ chối; address inactive không được Checkout chọn.
- Giỏ không cho quantity sai/Variant không mua được; không tin giá frontend.
- Dọn Cart không xóa dòng đã đổi version/quantity.
- Mã hết hạn, sai điều kiện, vượt quota, dùng lặp, trả lặp đều an toàn.
- Khóa/vô hiệu hóa user làm session cũ hết hiệu lực và có audit.

## Ràng buộc dữ liệu và tích hợp cần thực hiện

- Mã giảm giá được chọn khi xem trước đơn chưa giữ lượt dùng. Chỉ khi Hiếu bắt đầu tạo đơn mới tạo bản ghi giữ lượt và gắn mã giữ lượt vào phiên đặt hàng.
- Một phiên đặt hàng chỉ chọn tối đa một mã giảm tiền hàng và một mã giảm phí giao hàng.
- Nếu khách dùng mã trong ví, bản ghi giữ lượt phải liên kết đúng khách hàng và đúng mã đã được cấp; mã giảm giá công khai không cần bản ghi ví.
- Mọi số tiền giảm không âm; máy chủ kiểm tra cách giảm cố định/phần trăm, giới hạn dùng tổng và giới hạn theo khách trong cùng giao dịch.
- Giữ lượt chỉ được trả lại khi đang ở trạng thái đã giữ; mã đã dùng cho đơn được xác nhận không tự trả lại chỉ vì phát sinh lỗi giao hàng.
