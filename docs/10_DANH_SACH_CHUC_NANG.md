# DYNAMICMART — DANH SÁCH CHỨC NĂNG TỔNG HỢP

> Tài liệu này tổng hợp toàn bộ chức năng đã chốt để có thể copy làm mô tả đề tài, backlog hoặc phân công. Thuật ngữ kỹ thuật tiếng Anh được giữ trong dấu `` ` `` và có nghĩa tiếng Việt đi kèm.

## 1. Phạm vi sản phẩm

DynamicMart là sàn thương mại điện tử **B2C**: một doanh nghiệp bán hàng cho nhiều customer (khách hàng).

- Không có seller (người bán độc lập), store (gian hàng), commission (hoa hồng), payout (đối soát chi trả) hoặc tách Order theo nhiều người bán.
- Có hai role (vai trò) nghiệp vụ: `CUSTOMER` (khách hàng) và `ADMIN` (quản trị viên).
- Customer bắt buộc đăng nhập mới được Checkout (đặt hàng), tạo Order (đơn hàng), xem và dùng voucher (mã giảm giá).
- Guest (khách chưa đăng nhập) chỉ được xem sản phẩm và có thể dùng Guest Cart (giỏ hàng tạm) ở P1; không được đặt hàng.

## 2. Mức ưu tiên triển khai

| Mức | Ý nghĩa |
|---|---|
| **P0** | Bắt buộc hoàn thành để hệ thống có thể demo quy trình mua hàng đầy đủ. |
| **P1** | Mở rộng sau khi P0 ổn định. |
| **P2** | Hướng phát triển tương lai, không làm trong phiên bản hiện tại. |

---

## 3. Chức năng dành cho Customer (khách hàng)

### 3.1. Tài khoản và xác thực — P0

1. Đăng ký tài khoản customer.
2. Đăng nhập bằng email và mật khẩu.
3. Đăng xuất.
4. Làm mới phiên đăng nhập bằng `Refresh Token` (mã làm mới phiên).
5. Yêu cầu đặt lại mật khẩu khi quên; hệ thống luôn trả thông báo chung, không tiết lộ email có tồn tại hay không.
6. Đặt mật khẩu mới bằng token một lần có thời hạn; đổi xong hệ thống vô hiệu toàn bộ phiên cũ.
7. Xem và cập nhật hồ sơ cá nhân.
8. Chỉ xem và thao tác dữ liệu do chính customer sở hữu.
9. Không thể tự tạo tài khoản `ADMIN` từ API đăng ký công khai.

### 3.2. Sổ địa chỉ — P0

1. Xem danh sách địa chỉ giao hàng của bản thân.
2. Thêm địa chỉ.
3. Sửa địa chỉ.
4. Xóa địa chỉ không còn dùng.
5. Chọn một địa chỉ làm mặc định.
6. Mỗi địa chỉ giao hàng có:
   - người nhận;
   - số điện thoại;
   - địa chỉ chi tiết;
   - tỉnh/thành phố;
   - phường/xã.
7. Tỉnh/thành phố và phường/xã là dropdown lấy từ API danh mục GHN; Customer không nhập text hoặc mã địa giới tùy ý. Danh mục hiện hành dùng hai cấp Tỉnh/Thành phố → Phường/Xã, không có field quận/huyện.
8. Customer chỉ tự nhập số nhà/đường/ghi chú trong địa chỉ chi tiết.
9. Customer chỉ được chọn địa chỉ của mình khi Checkout.

### 3.3. Xem catalog sản phẩm — P0

1. Xem danh sách category (danh mục).
2. Duyệt category theo cấu trúc cha–con.
3. Xem danh sách sản phẩm theo category.
4. Xem trang chi tiết Product (sản phẩm).
5. Xem ảnh sản phẩm và ảnh theo Variant (phiên bản sản phẩm) khi có.
6. Xem mô tả, thuộc tính và thông số động của sản phẩm.
7. Chọn Variant, ví dụ màu, size, dung lượng hoặc cấu hình.
8. Khi đổi Variant, cập nhật đúng SKU, giá, tồn kho và ảnh tương ứng.
9. Tìm kiếm sản phẩm theo từ khóa.
10. Lọc sản phẩm theo thuộc tính động, category, giá hoặc điều kiện được cấu hình.
11. Sắp xếp sản phẩm theo rule hệ thống, ví dụ mới nhất, giá tăng/giảm hoặc bán chạy.
12. Hiển thị loading (đang tải), empty state (không có dữ liệu) và error state (lỗi tải dữ liệu).

### 3.4. Giỏ hàng — P0

1. Xem giỏ hàng của customer đã đăng nhập.
2. Thêm một Variant vào giỏ.
3. Cập nhật số lượng sản phẩm trong giỏ.
4. Xóa sản phẩm khỏi giỏ.
5. Chọn/bỏ chọn CartItem (dòng giỏ hàng) để Checkout.
6. Một Variant chỉ có một dòng trong một giỏ hàng; thêm lại thì tăng số lượng.
7. Hệ thống kiểm tra lại Product/Variant, giá và tồn kho ở backend trước Checkout.
8. Frontend không được tự quyết định giá hoặc tổng tiền.

### 3.5. Voucher và khuyến mãi — P0

#### A. Giảm giá trực tiếp trên sản phẩm

1. Quản trị viên tạo campaign giảm giá trực tiếp, chọn một hoặc nhiều Variant, cấu hình số tiền/phần trăm giảm và thời gian hiệu lực.
2. Giá niêm yết Variant không bị sửa. Hệ thống tính giá sale từ campaign còn hiệu lực.
3. Trang chủ, danh sách và chi tiết Product hiển thị giá niêm yết gạch ngang, giá sale và badge “Giảm x%” chỉ khi direct sale đang hiệu lực.
4. Không có hai direct sale đồng thời trên cùng một Variant trong P0.
5. Khi vào Checkout/Create Order, backend tính lại direct sale và lưu snapshot giá niêm yết, số tiền direct sale giảm, giá sau sale; không tin giá/badge từ frontend.
6. Direct sale được cộng với tối đa một voucher hàng hóa; voucher hàng hóa tính trên giá sau direct sale.

#### B. Voucher/mã giảm giá

1. Customer đã đăng nhập xem Voucher Wallet (ví mã giảm giá) của mình.
2. Customer xem voucher mặc định khi voucher đang hoạt động và đủ điều kiện sơ bộ.
3. Voucher có ba cách phân phối:
   - `DEFAULT_FOR_ELIGIBLE`: tất cả customer đã đăng nhập và đủ điều kiện sơ bộ đều thấy;
   - `ASSIGNED_ONLY`: chỉ customer được Admin/campaign cấp vào ví cá nhân mới thấy và dùng;
   - `CODE_ONLY`: Customer phải nhập mã hợp lệ mới dùng được.
4. Customer chọn hoặc bỏ chọn voucher trong Checkout.
5. Hiển thị voucher đủ điều kiện và voucher không đủ điều kiện kèm lý do.
6. Hệ thống chuẩn hóa voucher code trước khi kiểm tra.
7. Hệ thống kiểm tra server-side (tại backend):
   - voucher đang hoạt động;
   - thời gian bắt đầu/kết thúc;
   - tổng số lượt dùng;
   - số lượt dùng của một customer;
   - giá trị đơn tối thiểu;
   - giá trị hàng đủ điều kiện tối thiểu;
   - Product/Category/Danh sách Product được áp dụng.
8. Hỗ trợ các loại voucher:
   - `ORDER_DISCOUNT` (giảm giá toàn đơn);
   - `SHIPPING_DISCOUNT` (giảm phí vận chuyển/freeship);
   - `PRODUCT_DISCOUNT` (giảm một sản phẩm);
   - `CATEGORY_DISCOUNT` (giảm theo danh mục);
   - `PRODUCT_LIST_DISCOUNT` (giảm theo danh sách sản phẩm).
9. Hỗ trợ hai cách giảm giá:
   - `FIXED_AMOUNT` (giảm số tiền cố định);
   - `PERCENTAGE` (giảm theo phần trăm, có mức giảm tối đa).
10. P0 cho phép tối đa:
   - một voucher giảm hàng hóa; và
   - một voucher giảm phí ship.
11. Không cộng nhiều voucher giảm hàng hóa trên cùng phần tiền hàng.
12. Voucher freeship chỉ giảm phí ship, không giảm tiền sản phẩm.
13. Voucher không được làm tổng thanh toán âm.
14. Trang Product chỉ có thể hiển thị “có voucher” hoặc mức giảm tối đa; không gạch giá vì voucher. Giá sau voucher chỉ authoritative sau khi customer đăng nhập, chọn voucher và Checkout server tính lại.
15. Checkout Preview/Session chỉ kiểm tra điều kiện; khi customer bấm Tạo Order, Saga mới reserve quota voucher để không vượt số lượt dùng.
16. Hủy hoặc hết hạn Checkout draft trước Create Order không có quota để release; Saga lỗi hoặc thanh toán trả trước failed/hết hạn release quota an toàn.
17. Khi Voucher đã `CONSUMED` (đã dùng) cho Order xác nhận, quota không được trả lại chỉ vì giao vận gặp ngoại lệ.

### 3.6. Checkout (xác nhận đặt hàng) — P0

1. Chỉ customer đã đăng nhập được vào Checkout.
2. Chọn các CartItem cần mua hoặc đi từ Buy Now với đúng một Variant/số lượng đã chọn.
3. Chọn địa chỉ giao hàng đã lưu của chính customer. Nếu chưa có hoặc muốn tạo địa chỉ khác, customer thêm ngay tại Checkout; lưu thành công thì địa chỉ mới tự được chọn và còn lại trong Profile cho các lần sau.
4. Xem Checkout Preview (bản xem trước đơn hàng), gồm:
   - sản phẩm và Variant;
   - số lượng;
   - giá server;
   - giảm giá Product/Order;
   - phí ship GHN;
   - giảm phí ship;
   - tổng tiền cuối cùng.
5. Backend kiểm tra lại giá, trạng thái Product/Variant, tồn kho, voucher và địa chỉ trước khi cho tạo Order.
6. Backend gọi API GHN để lấy phí ship, dịch vụ giao và ETA (thời gian giao dự kiến).
7. Frontend không gửi hoặc sửa phí ship.
8. Nếu GHN không báo giá được, timeout hoặc địa chỉ không map được, hệ thống không cho tạo Order; customer có thể sửa địa chỉ hoặc thử lại.
9. Mỗi Checkout Session có thời hạn 15 phút.
10. Session là bản nháp: không reserve tồn kho/voucher chỉ vì mở Preview. Khi bấm Tạo Order, Saga reserve và bù trừ theo payment rule.
11. Customer có thể hủy Checkout Session **trước khi có Order**.
12. Hủy/hết hạn draft chỉ đóng session; Saga lỗi hoặc payment trả trước failed/hết hạn mới giải phóng reservation đúng một lần.
13. Hủy session không xóa CartItem, không tạo Payment (thanh toán), không tạo Order và không phát event hủy đơn.
14. Quote GHN mang fingerprint server-side theo customer, địa chỉ canonical, item/số lượng/trọng lượng, voucher ship, service và kho gửi. Đổi một input, quote hết hạn hoặc đã dùng phải quote lại; frontend không thể tái dùng phí cũ.

### 3.7. Đặt hàng và lịch sử đơn — P0

1. Tạo Order bằng request có `Idempotency-Key` (khóa chống tạo trùng khi bấm lại/retry).
2. Mỗi Checkout Session chỉ tạo tối đa một Order.
3. Order lưu snapshot bất biến của:
   - thông tin Product/Variant, SKU, ảnh, giá, số lượng;
   - địa chỉ giao hàng;
   - voucher và số tiền giảm;
   - phí ship GHN, dịch vụ GHN và ETA;
   - phương thức/thời điểm thanh toán;
   - toàn bộ giá trị tiền của đơn.
4. Customer xem danh sách Order của mình.
5. Customer xem chi tiết từng Order của mình.
6. Customer xem timeline (dòng thời gian) trạng thái Order.
7. Customer không xem được Order của customer khác.
8. Customer **không được hủy Order sau khi Order đã được tạo**, kể cả đổi ý hoặc quên đặt hàng.
9. Customer chỉ hủy Checkout Session khi Order chưa tồn tại.

### 3.8. Thanh toán — P0

Customer chọn một trong các tổ hợp hợp lệ sau:

| Thời điểm thanh toán | Phương thức | Ý nghĩa |
|---|---|---|
| `PREPAID` (trả trước) | `VNPAY` | Thanh toán VNPay trước khi hệ thống fulfillment (xử lý/giao đơn). |
| `POSTPAID` (trả sau) | `VNPAY` | Thanh toán VNPay khi shipper/admin sẵn sàng bàn giao hàng tại địa chỉ snapshot. |
| `POSTPAID` (trả sau) | `COD` | Thu tiền mặt khi giao hàng. |
| `NOT_REQUIRED` (không cần thanh toán) | `FREE` (đơn 0đ) | Backend tự gán khi tổng tiền cuối là 0đ. |

Các chức năng thanh toán cụ thể:

1. Tạo Payment Session (phiên thanh toán) cho đơn có tiền.
2. Tạo URL/QR VNPay cho đơn trả trước ngay sau khi tạo Order.
3. Tạo URL/QR VNPay trả sau khi đơn đến bước sẵn sàng bàn giao.
4. Customer thanh toán VNPay qua URL/QR gắn với chính Order, không dùng QR/tài khoản cá nhân của shipper.
5. Hệ thống nhận IPN (thông báo thanh toán từ VNPay), kiểm tra chữ ký, mã tham chiếu, số tiền và trạng thái.
6. Hệ thống chống callback/IPN trùng lặp.
7. URL/QR VNPay có thời hạn 15 phút.
8. Với trả trước, URL hết hạn sẽ hủy đơn chưa thanh toán và giải phóng tồn kho/voucher reservation đúng một lần.
9. IPN đến muộn sau khi trả trước hết hạn chỉ được lưu audit, không mở lại Order.
10. Với VNPay trả sau, QR hết hạn chỉ tạo QR mới khi Order đang chờ bàn giao; không hủy Order.
11. COD tạo Payment trạng thái chờ, không được đánh dấu đã trả tiền từ đầu.
12. Đơn 0đ không tạo Payment, không tạo URL VNPay/COD; backend tự xác nhận Order.
13. Frontend không được tự chọn `FREE` cho đơn có tổng tiền lớn hơn 0.
14. Hệ thống không chấp nhận `PREPAID + COD`.

### 3.9. Nhận hàng và hoàn tất Order — P0

1. Customer đã đăng nhập bấm **Đã nhận hàng** cho Order của chính mình sau khi nhận hàng.
2. Order Service kiểm tra ownership (quyền sở hữu), trạng thái hợp lệ và `Idempotency-Key` trước khi phát event `ShipmentDelivered` (đã giao hàng).
3. Với trả sau VNPay hoặc COD, Order chỉ `COMPLETED` (hoàn tất) khi có cả:
   - xác nhận thanh toán thành công; và
   - xác nhận đã giao hàng.
4. Hai event thanh toán/giao hàng có thể đến theo bất kỳ thứ tự nào; hệ thống vẫn chỉ hoàn tất Order đúng một lần.
5. Với trả trước VNPay hoặc đơn 0đ, customer xác nhận đã nhận hàng là điều kiện hoàn tất giao đơn theo state machine (máy trạng thái).
6. Không có Return/Refund tự động (trả hàng/hoàn tiền tự động) ở P0/P1.

### 3.10. Wishlist (danh sách yêu thích) — P1

1. Thêm Product vào Wishlist.
2. Xóa Product khỏi Wishlist.
3. Xem Wishlist của bản thân.
4. Một Product chỉ có một dòng trong Wishlist của một customer.

### 3.11. Buy Now (mua ngay) — P0

1. Customer chọn Variant và số lượng để mua ngay.
2. Tạo Checkout Session `source = BUY_NOW` chứa đúng lựa chọn tạm đó và dùng đầy đủ flow Checkout: chọn/lưu địa chỉ, voucher, GHN quote, TTL, reservation, thanh toán và Create Order.
3. Không tạo, sửa hoặc xóa CartItem trong giỏ hàng hiện tại.
4. Nếu guest bấm Mua ngay, hệ thống giữ `variantId`/số lượng trong cookie HttpOnly có chữ ký tối đa 15 phút, chuyển Login/Register rồi quay về đúng lựa chọn. Sau đăng nhập, backend kiểm tra lại Catalog trước khi tạo Checkout Session.
5. Với mua từ Cart, Cart chỉ dọn sau `OrderConfirmed`: chỉ xóa các dòng đã chọn có version/số lượng chưa đổi; Buy Now, dòng không chọn và dòng đã sửa trong lúc chờ payment luôn giữ nguyên.

### 3.12. Guest Cart (giỏ hàng khách) — P1

1. Guest thêm/xóa/sửa số lượng sản phẩm bằng anonymous token/cookie (mã ẩn danh/cookie khó đoán).
2. Sau khi login, hệ thống gộp Guest Cart vào Customer Cart theo Variant.
3. Không tạo dòng Variant trùng lặp.
4. Không vượt tồn kho hiện tại khi gộp.
5. Guest vẫn phải đăng nhập trước Checkout, tạo Order và xem voucher mặc định.

### 3.13. Review (đánh giá) — P0

1. Customer đánh giá sao từ 1 đến 5 cho OrderItem đã mua.
2. Customer viết nhận xét.
3. Customer có thể thêm ảnh đánh giá nếu giao diện triển khai.
4. Customer chỉ review khi:
   - Order thuộc về customer đó;
   - Order ở trạng thái `COMPLETED`;
   - OrderItem thuộc Order;
   - OrderItem chưa được review.
5. Mỗi OrderItem chỉ review một lần.
6. Customer không review sản phẩm chưa mua hoặc đơn của người khác.

### 3.14. Hỏi đáp sản phẩm — P1

1. Customer gửi câu hỏi cho Product.
2. Customer xem câu hỏi/câu trả lời đang hiển thị.
3. Customer không sửa dữ liệu của customer khác.

### 3.15. Thông báo — P1

1. Nhận thông báo trong hệ thống về Order, thanh toán hoặc hoạt động liên quan.
2. Xem danh sách thông báo.
3. Đánh dấu thông báo đã đọc.

### 3.16. Live Chat Customer–Admin — P2

1. Customer tạo và tiếp tục cuộc hội thoại hỗ trợ với `ADMIN` về sản phẩm/đơn hàng.
2. Admin có inbox, nhận/gán/đóng hội thoại và trả lời với tư cách quản trị viên.
3. Không có seller hoặc chat giữa customer với seller trong mô hình B2C này.

---

## 4. Chức năng dành cho Admin (quản trị viên)

### 4.1. Quản lý người dùng — P0

1. Xem danh sách customer và admin có phân trang.
2. Tìm kiếm user theo email, họ tên hoặc số điện thoại.
3. Lọc user theo role và status.
4. Xem hồ sơ cơ bản: tên, email, số điện thoại, role, status, thời điểm đăng nhập gần nhất và ngày tạo tài khoản.
5. Xem lịch sử thao tác quản lý tài khoản.
6. Khóa tạm thời tài khoản (`LOCKED`).
7. Mở khóa tài khoản về `ACTIVE`.
8. Vô hiệu hóa tài khoản (`DISABLED`) khi cần ngừng sử dụng lâu dài.
9. Đổi role giữa `CUSTOMER` và `ADMIN` bằng API quản trị được bảo vệ.
10. Khi khóa, vô hiệu hóa hoặc đổi role, hệ thống revoke Refresh Token và vô hiệu hóa JWT/session cũ theo `auth_version`.
11. Bắt buộc nhập lý do cho thao tác đổi role/status; hệ thống lưu audit bất biến.
12. Không hiển thị hoặc cho sửa password hash, Refresh Token hay token thô.
13. Không xóa vật lý user vì phải giữ lịch sử Order, Payment và audit.
14. Admin không được tự đổi role/status của chính mình.
15. Không được vô hiệu hóa hoặc giáng role admin đang hoạt động cuối cùng.

### 4.2. Quản lý Category và thuộc tính động — P0

1. Tạo, sửa, bật/tắt Category.
2. Tạo category cha–con.
3. Sắp xếp thứ tự Category.
4. Tạo, sửa, bật/tắt Attribute (thuộc tính), ví dụ màu, RAM, size.
5. Chọn kiểu Attribute:
   - text (văn bản);
   - number (số nguyên);
   - decimal (số thập phân);
   - boolean (đúng/sai);
   - select (chọn một);
   - multi-select (chọn nhiều).
6. Tạo/sửa/bật tắt option (lựa chọn) cho Attribute.
7. Gắn Attribute vào Category.
8. Cấu hình Attribute áp dụng cho Product hoặc Variant.
9. Cấu hình Attribute bắt buộc, cho phép lọc và thứ tự hiển thị.

### 4.3. Quản lý Product và Variant — P0

1. Tạo Product.
2. Sửa Product.
3. Lưu nháp, kích hoạt, ngừng bán hoặc lưu trữ Product.
4. Chọn Category cho Product.
5. Nhập mô tả ngắn, mô tả chi tiết và thuộc tính động.
6. Thêm/sửa/xóa ProductVariant.
7. Mỗi Variant có SKU duy nhất, giá, trạng thái và thứ tự hiển thị riêng.
8. Cập nhật ảnh Product và ảnh theo Variant.
9. Nhập cân nặng bắt buộc cho Variant để tính phí GHN.
10. Nhập kích thước gói hàng của Variant hoặc dùng kích thước mặc định của Product khi cần.
11. Variant thiếu cân nặng hoặc thông tin giao hàng hợp lệ không được kích hoạt/Checkout.

### 4.4. Quản lý tồn kho — P0

1. Xem tồn kho theo Variant.
2. Điều chỉnh tồn kho theo quyền admin.
3. Xem số hàng khả dụng, số hàng đang reserve và số hàng đã commit.
4. Hệ thống reserve tồn kho trong Saga Create Order, không khi customer chỉ mở Checkout Session/Preview.
5. Hệ thống commit tồn kho khi Order được xác nhận theo rule thanh toán.
6. Hệ thống release tồn kho khi Saga Create Order lỗi hoặc payment trả trước failed/hết hạn.
7. Chống oversell (bán vượt tồn kho) khi nhiều request mua cùng lúc.
8. Chống reserve/commit/release trùng lặp.
9. Nhận cảnh báo tồn kho thấp — P1.

### 4.5. Quản lý voucher/khuyến mãi — P0

1. Tạo voucher.
2. Cập nhật voucher.
3. Vô hiệu hóa voucher.
4. Cấu hình mã voucher, tên, mô tả, thời gian hiệu lực.
5. Cấu hình loại voucher, phạm vi áp dụng và cách giảm giá.
6. Cấu hình giảm tiền cố định hoặc giảm theo phần trăm.
7. Cấu hình mức giảm tối đa.
8. Cấu hình tổng lượt dùng và lượt dùng tối đa/customer.
9. Cấu hình giá trị đơn tối thiểu và giá trị hàng đủ điều kiện tối thiểu.
10. Gắn voucher với Product, Category hoặc danh sách Product khi cần.
11. Đánh dấu voucher mặc định để customer đã đăng nhập được xem khi đủ điều kiện.
12. Xem trạng thái voucher reservation/consume/release phục vụ vận hành.

### 4.6. Quản lý Order và fulfillment (xử lý đơn) — P0

1. Xem danh sách Order.
2. Lọc/tìm kiếm Order theo các tiêu chí giao diện đã triển khai.
3. Xem chi tiết Order, item snapshot, address snapshot, voucher snapshot, phí ship và lịch sử trạng thái.
4. Xác nhận Order (`CONFIRMED`) theo state machine.
5. Chuyển Order sang đóng gói (`PACKING`).
6. Chuyển Order sang đang giao (`SHIPPING`).
7. Với trả sau, chuyển Order sang chờ bàn giao/thanh toán (`HANDOVER_PENDING`) đúng rule.
8. Không đánh dấu `DELIVERED` thay customer; customer dùng chức năng **Đã nhận hàng** để phát `ShipmentDelivered`.
9. Xem lịch sử thay đổi trạng thái, actor (người/hệ thống thực hiện), lý do và correlation ID (mã liên kết luồng).
10. Không có nút cho customer hoặc admin hủy Order sau tạo trong P0. `CANCELLED` chỉ do hệ thống đặt khi Payment trả trước thất bại hoặc hết hạn lúc Order còn chờ thanh toán.

### 4.7. Vận hành thanh toán — P0

1. Xem trạng thái Payment: chờ, thành công, thất bại hoặc hết hạn.
2. Xem lịch sử Payment Attempt (lần tạo URL/QR hoặc xác nhận COD).
3. Xem audit callback VNPay: chữ ký hợp lệ, số tiền hợp lệ, callback trùng hoặc callback đến muộn.
4. Xác nhận thu COD theo luồng vận hành được bảo vệ.
5. Không được tự sửa số tiền thanh toán từ frontend/admin UI ngoài rule nghiệp vụ.
6. Không được đánh dấu Payment `PAID` trước khi COD được thu hoặc IPN VNPay hợp lệ.

### 4.8. Quản lý review, hỏi đáp và nội dung — P0/P1

1. Ẩn Review vi phạm và lưu lý do ẩn.
2. Xem danh sách Review theo Product nếu giao diện quản trị triển khai.
3. Xem/ẩn Question (câu hỏi) — P1.
4. Trả lời Question với tư cách admin — P1.

### 4.9. Báo cáo — P0/P1

1. Xem Revenue (doanh thu theo định nghĩa P0): tổng `final_total` của Order `COMPLETED`.
2. Xem Best Seller (sản phẩm/Variant bán tốt) từ Order hoàn tất.
3. Xem thống kê doanh số theo Category và Order — P1.
4. Không gọi Revenue là lợi nhuận vì chưa tính giá vốn, chi phí vận hành hoặc hoàn tiền.
5. Refund/Net Revenue (hoàn tiền/doanh thu thuần) là P2.

---

## 5. Chức năng hệ thống và tích hợp

### 5.1. API Gateway — P0

1. Frontend chỉ gọi API Gateway, không gọi trực tiếp business service.
2. Điều hướng request đến đúng service.
3. Kiểm tra JWT cơ bản ở edge (cổng vào).
4. Cấu hình CORS.
5. Chuẩn hóa lỗi trả về.
6. Gắn Correlation ID (mã liên kết request giữa nhiều service).
7. Rate limit (giới hạn tần suất) cơ bản.
8. Ghi log an toàn, không ghi secret/token/password.

### 5.2. GHN chỉ tính phí ship — P0

1. Payment Service đồng bộ/cache danh mục địa giới GHN và cung cấp API Tỉnh/Thành phố → Phường/Xã cho Profile/Checkout.
2. Frontend phải chọn địa giới từ danh mục này; chỉ số nhà/đường là input tự do. Backend validate ward thuộc đúng province trước khi lưu Address.
3. Payment Service gọi GHN từ backend để quote (báo giá) ship.
4. Input gồm địa chỉ nhận đã map GHN, tổng cân nặng, kích thước gói hàng và kho gửi cố định.
5. Nhận phí ship, GHN service ID và ETA.
6. Snapshot kết quả GHN vào Checkout Session và Order.
7. Không nhận shipping fee từ frontend.
8. Không dùng GHN để tạo vận đơn.
9. Không dùng GHN tracking (theo dõi giao hàng).
10. Không nhận webhook GHN trong scope hiện tại.
11. Token GHN, Shop ID và thông tin kho gửi nằm ở cấu hình triển khai; không đưa vào Git/log/frontend.

### 5.3. VNPay — P0

1. Tạo URL/QR có amount server-side, reference, TTL và chữ ký.
2. Xác thực IPN bằng checksum/chữ ký.
3. Kiểm tra reference, số tiền và trạng thái trước khi cập nhật Payment.
4. Lưu audit cho IPN hợp lệ, sai, trùng lặp hoặc đến muộn.
5. Mỗi callback hợp lệ chỉ tạo một business effect (tác động nghiệp vụ) dù bị gửi lại nhiều lần.
6. Gửi `PaymentSucceeded` qua Outbox khi Payment chuyển `PAID`. `PaymentFailed` hoặc `PaymentExpired` chỉ phát cho Payment trả trước khi khoản phải thu kết thúc thất bại/hết hạn.

### 5.4. RabbitMQ, Saga, Outbox và idempotency — P0

1. RabbitMQ dùng để truyền event bất đồng bộ giữa service.
2. Các event chính gồm:
   - `OrderCreated` (đơn được tạo);
   - `OrderConfirmed` (đơn được xác nhận);
   - `OrderCancelled` (đơn bị hủy do rule hệ thống);
   - `OrderCompleted` (đơn hoàn tất);
   - `PaymentSucceeded` (thanh toán thành công);
   - `PaymentFailed` (thanh toán thất bại);
   - `PaymentExpired` (thanh toán hết hạn);
   - `PaymentDue` (Order Service yêu cầu tạo URL/QR cho `POSTPAID + VNPAY` khi đơn vào `HANDOVER_PENDING`; không dùng cho COD);
   - `ShipmentDelivered` (đã giao hàng);
   - `ReviewCreated`, `ReviewHidden` (tạo/ẩn review);
   - `ProductQuestionCreated` (tạo câu hỏi Product) — P1.
3. Dùng Saga (chuỗi giao dịch phân tán có bù trừ) cho Checkout:
   - validate Cart/Address/Product;
   - reserve voucher;
   - reserve inventory;
   - tạo Order;
   - xử lý theo payment timing/method;
   - commit hoặc release reservation khi thành công/thất bại.
4. Dùng Outbox: cập nhật nghiệp vụ và insert event trong cùng local transaction (giao dịch cục bộ).
5. Dùng `processed_events` để event trùng chỉ được xử lý một lần.
6. Dùng `Idempotency-Key` để request tạo Order hoặc command nội bộ retry không tạo tác động lặp.
7. Có retry, dead-letter queue/DLQ (hàng đợi lỗi) và audit cho lỗi publish/consume event.

### 5.5. Bảo mật và ownership (quyền sở hữu dữ liệu) — P0

1. Password chỉ lưu hash.
2. Refresh Token chỉ lưu hash.
3. Không log token, secret, password hoặc dữ liệu nhạy cảm không cần thiết.
4. Customer chỉ đọc/sửa Cart, Address, Order, Wishlist, Review và dữ liệu của chính mình.
5. API admin yêu cầu role `ADMIN`.
6. API nội bộ giữa service yêu cầu xác thực service-to-service.
7. Mỗi service chỉ đọc/ghi database của chính service đó.
8. Không có cross-service foreign key (khóa ngoại xuyên database).

### 5.6. Quan sát, lỗi và vận hành — P0

1. Ghi correlation ID xuyên Gateway, service và event.
2. Log lỗi theo chuẩn, không lộ dữ liệu nhạy cảm.
3. Có trạng thái loading/error/empty trên frontend.
4. Có audit cho payment, trạng thái Order, callback VNPay và event xử lý.
5. Có test retry, event trùng lặp, RabbitMQ down và Outbox publish lại.

---

## 6. State machine (máy trạng thái) cần hỗ trợ

### 6.1. Checkout Session — P0

```text
ACTIVE (đang hoạt động)
→ COMPLETED (đã tạo Order)

ACTIVE
→ CANCELLED (customer hủy trước khi có Order)

ACTIVE
→ EXPIRED (hết hạn 15 phút)
```

### 6.2. Quy ước trạng thái giao hàng và hoàn tất — P0

P0 không có dịch vụ giao vận, mã vận đơn hay theo dõi GHN. `DELIVERED` chỉ có nghĩa là chính customer đã bấm **Đã nhận hàng**; admin không được tự đặt trạng thái này.

```text
CONFIRMED (đã xác nhận)
→ PACKING (đang đóng gói)
→ SHIPPING (đang giao)

SHIPPING
→ DELIVERED (customer xác nhận đã nhận hàng)
```

Với đơn trả trước hoặc đơn 0đ, customer chỉ được bấm **Đã nhận hàng** khi đơn ở `SHIPPING`. Với đơn trả sau, admin chuyển `SHIPPING → HANDOVER_PENDING` (chờ bàn giao và thu tiền); customer chỉ được bấm **Đã nhận hàng** khi đơn ở `HANDOVER_PENDING`. Cả hai trường hợp đều ghi nhận `DELIVERED`.

`COMPLETED` chỉ được tạo khi đã có `ShipmentDelivered` và điều kiện thanh toán đã đạt. Nếu cả hai điều kiện đã có tại thời điểm customer xác nhận, hệ thống ghi lịch sử `DELIVERED` rồi chuyển ngay sang `COMPLETED` trong cùng transaction.

### 6.3. Đơn trả trước VNPay — P0

```text
PENDING_PAYMENT (chờ thanh toán)
→ CONFIRMED
→ PACKING
→ SHIPPING
→ DELIVERED
→ COMPLETED

PENDING_PAYMENT
→ CANCELLED (VNPay thất bại hoặc hết hạn)
```

Payment thành công xác nhận Order, chốt tồn kho và consume Voucher. Customer xác nhận đã nhận hàng mới tạo `ShipmentDelivered`; vì tiền đã thu, Order hoàn tất ngay sau đó.

### 6.4. Đơn trả sau VNPay/COD — P0

```text
CONFIRMED
→ PACKING
→ SHIPPING
→ HANDOVER_PENDING
→ DELIVERED
→ COMPLETED
```

`PaymentSucceeded` và `ShipmentDelivered` có thể đến theo bất kỳ thứ tự nào. Nếu chưa đủ cả hai, Order dừng ở `HANDOVER_PENDING` hoặc `DELIVERED`; khi đủ cả hai thì chuyển `COMPLETED` đúng một lần.

### 6.5. Đơn 0đ — P0

```text
CONFIRMED
→ PACKING
→ SHIPPING
→ DELIVERED
→ COMPLETED
```

Đơn 0đ dùng `NOT_REQUIRED + FREE`; không tạo Payment ngoài hệ thống. Customer xác nhận đã nhận hàng là điều kiện duy nhất còn thiếu để hoàn tất.

### 6.6. Trạng thái Payment và Payment Attempt — P0

`payments.status` là trạng thái của khoản phải thu cho cả đơn, còn `payment_attempts.status` là trạng thái của từng đường dẫn VNPay/lần ghi nhận COD.

| Loại thanh toán | Trạng thái Payment hợp lệ | Quy tắc |
|---|---|---|
| Trả trước VNPay | `PENDING` → `PAID`, `FAILED` hoặc `EXPIRED` | `FAILED`/`EXPIRED` là kết thúc: phát event tương ứng để hủy Order và trả reservation. |
| Trả sau VNPay | `PENDING` → `PAID` | URL/QR thất bại hoặc hết hạn chỉ làm Attempt `FAILED`/`EXPIRED`; Payment vẫn `PENDING` để tạo URL/QR mới. |
| Trả sau COD | `PENDING` → `PAID` | Chỉ chuyển `PAID` sau xác nhận thu tiền có quyền, đúng số tiền và có biên nhận. |
| Đơn 0đ | Không tạo Payment | Điều kiện thanh toán được xem là đã đạt. |

`PaymentDue` do Order Service phát khi Order `POSTPAID + VNPAY` chuyển `HANDOVER_PENDING`; Payment Service nhận event này để tạo Attempt VNPay trả sau. COD không dùng event này, chỉ được ghi nhận thu tiền tại bàn giao. `PaymentFailed` và `PaymentExpired` chỉ phát cho Payment trả trước; Payment Service phát `PaymentSucceeded` đúng một lần khi khoản phải thu chuyển `PAID`.

---

## 7. Chức năng không nằm trong scope hiện tại

Các chức năng dưới đây **không triển khai** trong P0/P1, chỉ xem là P2 hoặc hướng mở rộng:

1. Marketplace nhiều seller/store.
2. Commission, payout, đối soát seller.
3. Tách một giỏ hàng thành nhiều Order theo seller.
4. Multi-warehouse (nhiều kho gửi).
5. Tạo vận đơn GHN, tracking GHN và webhook GHN.
6. Return (trả hàng) tự động.
7. Refund (hoàn tiền) tự động.
8. Loyalty point (điểm thưởng), ví điện tử/wallet.
9. Recommendation (gợi ý sản phẩm) nâng cao.
10. Search nâng cao/AI/RAG.
11. Refund/Net Revenue trong reporting.
12. Hóa đơn VAT điện tử, nếu chưa được chốt thành scope riêng.
13. Live Chat Customer–Admin (P2).

---

## 8. Tiêu chí demo P0 tối thiểu

```text
Đăng ký/Đăng nhập
→ Xem Product và chọn Variant
→ Chọn **Mua ngay** hoặc thêm Cart
→ Xem voucher mặc định sau đăng nhập
→ Chọn địa chỉ GHN hợp lệ
→ Checkout Preview lấy phí GHN
→ Chọn voucher
→ Reserve voucher + tồn kho
→ Tạo Order bằng Idempotency-Key
→ Thanh toán VNPay/COD hoặc đơn 0đ
→ Admin xử lý Confirm/Packing/Shipping
→ Customer xác nhận đã nhận hàng
→ Order Completed
→ Customer Review
→ Revenue/Best Seller cập nhật từ OrderCompleted
```
