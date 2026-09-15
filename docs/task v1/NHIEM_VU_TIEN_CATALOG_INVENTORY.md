# TIẾN — SẢN PHẨM, BIẾN THỂ VÀ TỒN KHO

Tiến sở hữu Catalog/Inventory trong `catalog-service`, `catalog_db` và giao diện đăng ký/đăng nhập/quên mật khẩu. Hạ tầng xác thực backend trong `identity-service` và Gateway do trưởng nhóm sở hữu; Tiến chỉ gọi Auth API theo contract nền chung, không sửa module `auth/`. Tiến không làm giỏ hàng, mã giảm giá, đặt hàng, thanh toán hoặc phí giao hàng.

## Chức năng được giao

### Giao diện xác thực — P0

Tiến làm các trang đăng ký, đăng nhập, đăng xuất, quên/đặt lại mật khẩu, trạng thái loading/lỗi và điều hướng quay về Mua ngay. Các trang chỉ gọi Auth API nền chung; không chứa business rule JWT, refresh token, mật khẩu hoặc Gateway.

### UC-05 — Duyệt, tìm kiếm, lọc và sắp xếp sản phẩm — P0

Khách hàng có thể:

- Xem sản phẩm mới, sản phẩm nổi bật và danh sách theo danh mục.
- Tìm kiếm theo tên, mã sản phẩm hoặc từ khóa được lập chỉ mục.
- Lọc theo danh mục, khoảng giá và các thuộc tính do danh mục cấu hình.
- Ví dụ thuộc tính có thể là màu sắc, kích cỡ, dung lượng, vật liệu, thương hiệu hoặc cấu hình; không viết riêng bộ lọc cho Fashion/Watch.
- Sắp xếp theo mới nhất, giá tăng/giảm hoặc bán chạy.
- Phân trang, giữ điều kiện tìm kiếm/lọc/sắp xếp khi chuyển trang.

Quy tắc nghiệp vụ:

- Chỉ hiện Product đang hoạt động, đã đến thời điểm công khai và có ít nhất một ProductVariant mua được.
- Giá hiển thị lấy từ Variant. Nếu có nhiều Variant thì hiển thị khoảng giá hoặc Variant đại diện theo quy tắc đã chốt, không ghép giá của hai Variant khác nhau.
- Nếu Promotion Engine trả direct sale đang hiệu lực cho Variant, hiển thị giá niêm yết gạch ngang, giá sale, badge phần trăm và thời hạn sale. Không hiển thị giá sau voucher tại Product Card vì voucher phụ thuộc account/Checkout.
- Variant hết hàng vẫn có thể hiện ở trang chi tiết nhưng không được mua; Product không có Variant mua được không hiện trong kết quả mua hàng.
- API phải phân trang trên máy chủ; không tải toàn bộ dữ liệu về trình duyệt để lọc.
- Tránh truy vấn lặp khi lấy danh mục, ảnh, thuộc tính và Variant.

### UC-06 — Hiển thị sản phẩm bán chạy — P0

Tiến hiển thị sản phẩm bán chạy tại:

- Trang chủ.
- Danh sách sản phẩm hoặc khu vực nổi bật.
- Thẻ sản phẩm.

Tùng cung cấp dữ liệu bán chạy từ báo cáo. Chỉ tính các dòng hàng của Order `COMPLETED`; không tự đếm từ Giỏ hàng, thanh toán thành công hoặc trạng thái đang giao.

### UC-07 — Trang chi tiết sản phẩm, chọn biến thể và Mua ngay — P0

Trang chi tiết hiển thị:

- Tên, mô tả, danh mục, ảnh và thuộc tính sản phẩm.
- Đánh giá đã được phép hiển thị.
- Các lựa chọn để xác định Variant, ví dụ màu/kích cỡ/dung lượng.
- Mã hàng, giá, ảnh, cân nặng và tồn kho thay đổi theo Variant được chọn.
- Khi đổi Variant, lấy lại giá niêm yết/direct sale tương ứng; giá sale chỉ là response server, không tự tính trên trình duyệt.
- Trạng thái còn hàng/hết hàng, giới hạn số lượng hợp lệ.

Khách hàng không được thêm giỏ hoặc Mua ngay khi:

- Chưa chọn đủ thuộc tính bắt buộc.
- Không xác định được Variant.
- Variant ngừng bán hoặc hết hàng.
- Số lượng nhỏ hơn 1 hoặc vượt số lượng có thể bán.

Nút **Mua ngay**:

- Chỉ gửi `variantId` và số lượng; không gửi giá, tồn kho hay tổng tiền.
- Khách chưa đăng nhập được chuyển Login/Register và quay về đúng Variant/số lượng trong tối đa 15 phút.
- Mua ngay không tạo, sửa hoặc xóa dòng Giỏ hàng.

### UC-16 — Quản trị danh mục hàng hóa và tồn kho — P0

Quản trị viên có thể quản lý:

- Danh mục cha–con.
- Thuộc tính, lựa chọn thuộc tính và quy tắc thuộc tính theo danh mục.
- Product, ảnh sản phẩm và ProductVariant.
- Mã hàng, giá, cân nặng, kích thước gói hàng, trạng thái bán và thứ tự hiển thị.
- Tồn thực có, tồn đang giữ, tồn đã chốt; điều chỉnh tồn theo quyền.

Quy tắc nghiệp vụ:

- Mã danh mục, đường dẫn thân thiện và mã hàng phải duy nhất theo rule dữ liệu đã chốt.
- Giá, cân nặng, kích thước và tồn kho không âm; Variant bán được phải có cân nặng lớn hơn 0 để tính phí GHN.
- Chỉ Attribute được gắn với danh mục mới được nhập cho Product/Variant của danh mục đó.
- Không xóa cứng Product/Variant đã xuất hiện trong Order; chuyển sang ngừng bán hoặc lưu trữ.
- Ảnh phải kiểm tra kiểu tệp, dung lượng, thứ tự và liên kết đúng Product/Variant.

### UC-17 — Giữ, chốt và trả tồn kho — P0

- Chỉ Hiếu gọi giữ tồn trong Saga khi khách bấm Tạo Order; mở trang Checkout không được giữ hàng.
- Đơn trả trước giữ tồn đến khi thanh toán thành công/thất bại/hết hạn.
- Đơn trả sau hoặc đơn 0 đồng chốt tồn ngay khi OrderConfirmed.
- Saga lỗi, thanh toán trả trước thất bại hoặc hết hạn phải trả tồn đúng một lần.
- Hai khách cùng mua sản phẩm cuối chỉ một người giữ thành công.

## Giao diện cần làm

### Giao diện khách hàng

- Trang chủ: sản phẩm mới, bán chạy, danh mục nổi bật.
- Danh sách sản phẩm, thanh tìm kiếm, lọc máy tính/điện thoại, sắp xếp và phân trang.
- Thẻ sản phẩm có ảnh, giá niêm yết/giá sale khi có, trạng thái còn hàng, nhãn bán chạy và nhãn “Có voucher” nếu server trả eligibility sơ bộ.
- Trang chi tiết, thư viện ảnh, chọn Variant, hiển thị mã hàng/giá/tồn kho.
- Nút Thêm vào giỏ/Mua ngay, trạng thái đang tải, hết hàng, không có kết quả và lỗi tải dữ liệu.

### Giao diện quản trị

- Danh sách và form tạo/sửa danh mục, thuộc tính, lựa chọn thuộc tính.
- Danh sách và form tạo/sửa Product, Variant, ảnh và trạng thái.
- Màn hình tồn kho theo Variant và lịch sử điều chỉnh nếu đã mở trong P0.

## Phần làm ngay và phần cần chờ

| Làm ngay | Chờ để nối thật |
|---|---|
| Toàn bộ trang Catalog, lựa chọn Variant, form quản trị và dữ liệu mẫu | Thảo nối Thêm giỏ; Hiếu nối Mua ngay/giữ tồn; Tùng nối đánh giá và bán chạy thật |

## Dữ liệu bàn giao

- Cho Thảo: ProductSummary, thông tin Variant có thể thêm giỏ.
- Cho Hiếu: kiểm tra Variant mua được, giữ/chốt/trả tồn.
- Nhận từ Tùng: dữ liệu bán chạy, sự kiện tạo/ẩn đánh giá nếu cần cập nhật điểm đánh giá.
- Nhận từ Thảo: contract direct sale theo `variantId` gồm giá sale, số giảm, phần trăm và thời hạn; trả ProductSummary/ProductDetail đã gộp giá cho UI.

## Kiểm thử bắt buộc

- Tìm kiếm, lọc kết hợp, sắp xếp và phân trang đúng.
- Chỉ hiện Product/Variant đúng trạng thái mua được.
- Chọn thuộc tính trả đúng Variant, giá, ảnh và tồn kho.
- Mã hàng trùng, giá/tồn/cân nặng sai bị từ chối.
- Variant hết hàng/ngừng bán không thêm Giỏ hàng hoặc Mua ngay.
- Hai yêu cầu mua sản phẩm cuối không làm tồn âm.
- Giữ/chốt/trả tồn gửi lặp không tạo tác động lần hai.
- Product có Order cũ không thể xóa cứng.

## Ràng buộc dữ liệu và tích hợp cần thực hiện

- Danh mục cha–con không được tạo vòng lặp.
- Ảnh của biến thể phải thuộc đúng sản phẩm cha; mỗi sản phẩm hoặc biến thể chỉ có một ảnh chính.
- Mọi điều chỉnh tồn kho của quản trị viên phải lưu lịch sử: lượng tăng/giảm, tồn trước/sau, lý do, người thực hiện và mã chống lặp.
- Khi điều chỉnh tồn, phải khóa dòng tồn kho; số tồn sau điều chỉnh không được nhỏ hơn số hàng đang giữ cho các đơn.
- Chuỗi làm mới phiên đăng nhập phải liên kết đúng token cũ với token thay thế; không được trỏ tới token không tồn tại.
