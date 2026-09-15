# DYNAMICMART — PHÂN CÔNG CÔNG VIỆC

> **Tài liệu nguồn hiện hành:** `00_OVERVIEW.md`; năm file phân công trong `task v1/`; `06_PHAN_LAM_CHUNG.md`, `07_CAI_DAT_PHAN_MEM.md`, `09_DATABASE_DESIGN.md`, `10_DANH_SACH_CHUC_NANG.md`, `11_BACKEND_CODING_RULES.md`, `12_FRONTEND_CODING_RULES.md` và `13_AUTHENTICATION_FOUNDATION.md` là phạm vi, phân công, thiết kế và luật triển khai được nhóm dùng để thực hiện. Nếu có mâu thuẫn với `DYNAMICMART_PHAM_VI_KIEN_TRUC_VA_PHAN_CONG.md`, ưu tiên bộ tài liệu này.

## 1. Mục tiêu
Nhóm gồm **5 thành viên** và chia việc theo từng **mảng nghiệp vụ**.

DynamicMart là mô hình **một doanh nghiệp bán trực tiếp cho nhiều khách hàng**. Quản trị viên (`ADMIN`) quản lý người dùng, sản phẩm, khuyến mãi và xử lý đơn; không có người bán hay cửa hàng độc lập, chia hoa hồng, chi trả cho người bán hoặc tách đơn theo người bán.

Mỗi người chịu trách nhiệm từ đầu đến cuối:
```text
Phân tích nghiệp vụ
→ Giao diện
→ Phần xử lý máy chủ và điểm gọi dữ liệu
→ Quy tắc nghiệp vụ
→ Tích hợp
→ Kiểm thử từng phần
→ Kiểm thử kết nối giữa các phần
→ Tài liệu
→ Demo
```

Kiến trúc cơ sở dữ liệu, cấu trúc dữ liệu gọi giữa các phần, cổng API (điểm tiếp nhận và điều hướng yêu cầu), RabbitMQ (hàng đợi chuyển sự kiện), Docker, Git và kiểm thử toàn luồng là **phần làm chung**, không tính vào nhiệm vụ riêng.

---

## 2. Kiến trúc
```text
Cổng API

1. Dịch vụ tài khoản
2. Dịch vụ danh mục sản phẩm
3. Dịch vụ giỏ hàng và mã giảm giá
4. Dịch vụ đơn hàng
5. Dịch vụ thanh toán và tính phí giao hàng
6. Dịch vụ tương tác khách hàng và báo cáo
```

Mã giảm giá là một phần của dịch vụ giỏ hàng do Thảo phụ trách, không tách thành dịch vụ hay cơ sở dữ liệu thứ bảy ở P0. GHN chỉ dùng để cung cấp danh mục Tỉnh/Thành phố → Phường/Xã và báo giá phí giao hàng tại máy chủ; chưa tạo vận đơn hay theo dõi giao hàng GHN.

Tổng:
```text
6 dịch vụ nghiệp vụ
1 cổng API
6 cơ sở dữ liệu tách theo từng dịch vụ
```

Các cơ sở dữ liệu:
```text
identity_db
catalog_db
cart_db
order_db
payment_db
engagement_db
```

---

## 3. Phạm vi nâng cấp theo ưu tiên

DynamicMart là hệ thống B2C đa ngành hàng, tập trung vào hành trình mua hàng tin cậy và không mở rộng sang marketplace đa seller.

### P0 — bắt buộc

- Biến thể sản phẩm là đơn vị bán được, có mã hàng (SKU), giá, tồn và thuộc tính/ảnh riêng khi cần.
- Danh mục và thuộc tính linh hoạt, không bị giới hạn ở thời trang hay đồng hồ.
- Giỏ hàng, phiên xác nhận đặt hàng, hủy phiên trước khi tạo đơn, bản chụp đơn và chống tạo đơn trùng; chỉ khách đã đăng nhập mới được đặt hàng hoặc xem mã giảm giá mặc định.
- Giữ/chốt/trả tồn kho và lượt dùng mã giảm giá **chỉ khi tạo đơn**, không giữ khi khách chỉ xem trước. Nếu một bước lỗi, chuỗi giao dịch phải tự bù trừ; sự kiện chờ gửi và sự kiện nhận trùng phải được xử lý an toàn.
- Thanh toán gồm trả trước VNPay, trả sau VNPay, trả sau COD, hoặc đơn 0 đồng. Đường dẫn VNPay và phiên đặt hàng có hạn 15 phút; phí giao hàng do máy chủ tính qua GHN từ một kho gửi cố định. Địa chỉ chỉ chọn Tỉnh/Thành phố → Phường/Xã từ danh mục GHN, còn số nhà/đường được nhập tự do.
- **Mua ngay** dùng cùng luồng đặt hàng với giỏ hàng nhưng chỉ chứa biến thể và số lượng vừa chọn, không thay đổi giỏ hàng.
- Khách chưa đăng nhập bấm Mua ngay được giữ lựa chọn ngắn hạn, sau đăng nhập hệ thống kiểm tra lại trước khi tạo phiên đặt hàng.
- Báo giá GHN gắn với đúng khách, địa chỉ và hàng hóa; chỉ dọn mặt hàng khỏi giỏ sau khi đơn được xác nhận và không xóa thay đổi mới.

### P1 — mở rộng sau P0

- Giỏ khách chưa đăng nhập và gộp giỏ sau đăng nhập.
- Thông báo, cảnh báo sắp hết hàng; GHN vẫn chỉ dùng để tính phí giao hàng.
- Hỏi đáp sản phẩm, danh sách yêu thích và báo cáo mở rộng.

### P2 — chỉ làm khi P0/P1 ổn định

- Gợi ý và tìm kiếm nâng cao.
- Nhiều kho, điểm tích lũy/ví, tự động trả hàng/hoàn tiền và trợ lý AI.
- Trò chuyện trực tiếp giữa khách hàng và quản trị viên, không phải với người bán.

---

## 4. Phân chia 5 phần việc

| Người | Thành viên | Phần việc phụ trách | Phạm vi chính |
|---|---|---|
| Phần nền chung | Trưởng nhóm | Xác thực backend và cổng API | JWT, refresh-token rotation, API đăng ký/đăng nhập/đăng xuất/quên mật khẩu, contract xác thực, Gateway security/routing. |
| Người 1 | Tiến | Giao diện tài khoản, sản phẩm, biến thể và tồn kho | UI đăng nhập/đăng ký/quên mật khẩu; danh mục; thuộc tính; sản phẩm; biến thể; tìm kiếm; hiển thị giá sale; giữ, chốt và trả tồn kho. |
| Người 2 | Thảo | Hồ sơ khách, địa chỉ, giỏ hàng và khuyến mãi | Hồ sơ; quản lý người dùng; sổ địa chỉ; giỏ hàng; direct sale; voucher cá nhân/mặc định; giỏ khách vãng lai ở P1. |
| Người 3 | Hiếu | Xác nhận đặt hàng và đơn hàng | Phiên đặt hàng; Mua ngay; tạo đơn; bản chụp đơn; trạng thái đơn; chuỗi giao dịch có bù trừ. |
| Người 4 | Hưng | Thanh toán và tính phí giao hàng | COD; VNPay; xác nhận thanh toán; danh mục địa giới GHN; báo giá phí giao hàng. |
| Người 5 | Tùng | Tương tác khách hàng và báo cáo | Đánh giá; hỏi đáp; yêu thích; thông báo; doanh thu; sản phẩm bán chạy; trò chuyện ở P2. |

Nhiệm vụ thực thi riêng của từng người:

- [Tiến — Giao diện tài khoản, danh mục sản phẩm và tồn kho](task%20v1/NHIEM_VU_TIEN_CATALOG_INVENTORY.md)
- [Thảo — Hồ sơ, giỏ hàng và mã giảm giá](task%20v1/NHIEM_VU_THAO_IDENTITY_CART_VOUCHER.md)
- [Hiếu — Xác nhận đặt hàng và đơn hàng](task%20v1/NHIEM_VU_HIEU_CHECKOUT_ORDER_SAGA.md)
- [Hưng — Thanh toán và GHN](task%20v1/NHIEM_VU_HUNG_PAYMENT_GHN.md)
- [Tùng — Tương tác khách hàng và báo cáo](task%20v1/NHIEM_VU_TUNG_ENGAGEMENT_REPORTING.md)

---

## 5. Phần nền chung — Xác thực và cổng API

Trưởng nhóm sở hữu phần nền **backend** dùng chung trong `identity-service` và `api-gateway`:

- Đăng ký, đăng nhập, đăng xuất, refresh-token rotation, quên/đặt lại mật khẩu và JWT contract.
- Băm mật khẩu/refresh token, HTTP-only refresh cookie, access token ngắn hạn, issuer/role/auth version.
- Cấu hình kiểm tra JWT tại Gateway, CORS, route và lỗi xác thực chuẩn.
- CSDL Identity/Flyway thuộc phần nền; Thảo vẫn sở hữu Profile, Address, Admin User Management trong cùng service nhưng không sửa module `auth/`.

Đây là nền để các domain dùng chung, không tính vào một nhiệm vụ riêng. Thay đổi DTO/auth claim/cookie phải do trưởng nhóm review.

---

## 6. Người 1 — Tiến: giao diện tài khoản, sản phẩm, biến thể và tồn kho

Tiến làm trọn luồng Catalog/Inventory và **giao diện** tài khoản theo Auth API nền chung:

- Danh mục sản phẩm, thuộc tính linh hoạt, sản phẩm và biến thể sản phẩm. Biến thể là phiên bản có thể bán riêng, có mã hàng (SKU), giá, tồn kho và ảnh riêng.
- Giao diện đăng ký, đăng nhập, đăng xuất, quên/đặt lại mật khẩu, validation và điều hướng quay về Mua ngay; không sửa `identity-service/auth` hay Gateway.
- Nhận giá sale đang hiệu lực từ Promotion Engine của Thảo để hiển thị đúng giá niêm yết, giá sale, phần trăm giảm và thời hạn trên Product Card/Detail; không tự tính sale ở frontend.
- Trang danh sách, tìm kiếm, lọc, sắp xếp, chi tiết sản phẩm và khu vực quản trị sản phẩm/tồn kho.
- Nút **Mua ngay**: chỉ chuyển lựa chọn hợp lệ sang bước xác nhận đặt hàng; không tự thay đổi giỏ hàng.
- Giữ tồn, chốt trừ tồn và trả tồn khi luồng tạo đơn thành công hoặc thất bại; phải chống bán vượt tồn khi nhiều khách mua cùng lúc.

Kiểm thử bắt buộc: dữ liệu thuộc tính; tìm kiếm/lọc; mã hàng không trùng; giá/tồn không âm; nhiều yêu cầu mua cùng lúc; và thao tác giữ/chốt/trả tồn gửi lại không bị thực hiện hai lần.

---

## 7. Người 2 — Thảo: hồ sơ, giỏ hàng và mã giảm giá

Thảo làm trọn luồng cho các việc sau:

- Hồ sơ, sổ địa chỉ và quyền sở hữu dữ liệu của khách hàng.
- Khu vực quản trị người dùng: tìm kiếm, khóa/mở khóa, vô hiệu hóa, đổi quyền và lưu lịch sử thao tác.
- Giỏ hàng: thêm, sửa số lượng, xóa, chọn mặt hàng để mua và dọn đúng các mặt hàng đã mua. Giỏ khách chưa đăng nhập và gộp giỏ là P1.
- Khuyến mãi: quản trị direct sale theo Variant; ví mã của khách, màn hình chọn mã, khu vực quản trị mã và kiểm tra điều kiện ở máy chủ.
- Voucher có thể là mặc định cho customer đã đăng nhập/đủ điều kiện, chỉ cấp cho từng account, hoặc chỉ dùng sau khi nhập mã.
- Các loại giảm gồm giảm toàn đơn, giảm phí giao hàng, giảm sản phẩm, giảm theo danh mục và giảm theo danh sách sản phẩm. Có thể giảm số tiền cố định hoặc giảm theo phần trăm có mức trần.
- Giữ lượt dùng mã chỉ khi khách bấm tạo đơn; xác nhận dùng hoặc trả lại lượt dùng theo kết quả thanh toán. Không cho áp dụng đồng thời nhiều mã giảm trên cùng tiền hàng.

Kiểm thử bắt buộc: quyền và chủ sở hữu; địa chỉ/giỏ của người khác; số lượng không hợp lệ hoặc mặt hàng trùng; mọi điều kiện mã giảm; giới hạn tổng và theo khách; và thao tác giữ/xác nhận dùng/trả lượt gửi lại không bị thực hiện hai lần.

---

## 8. Người 3 — Hiếu: xác nhận đặt hàng và đơn hàng

Hiếu làm trọn luồng cho các việc sau:

- Trang xác nhận đặt hàng, lịch sử đơn, chi tiết đơn, dòng thời gian trạng thái và màn hình xử lý đơn của quản trị viên.
- Tạo phiên xác nhận đặt hàng từ giỏ hoặc **Mua ngay**; cho phép hủy phiên trước khi đơn được tạo.
- Kiểm tra lại địa chỉ, sản phẩm, giá, tồn kho, mã giảm giá và phí giao hàng trước khi tạo đơn.
- Tạo đơn chỉ một lần cho mỗi phiên, kể cả khi khách bấm lại. Đơn phải lưu bản chụp bất biến của hàng hóa, địa chỉ, giảm giá, phí giao và thanh toán.
- Điều phối chuỗi giao dịch có bù trừ: nếu một bước tạo đơn thất bại thì trả lại tồn kho và lượt dùng mã giảm giá đúng một lần.
- Phối hợp trạng thái đơn với thanh toán: trả trước, trả sau và đơn 0 đồng.

Kiểm thử bắt buộc: dữ liệu xác nhận đặt hàng không hợp lệ; tạo đơn lặp; bản chụp đơn; chuyển trạng thái đúng/sai; bù trừ khi lỗi; và sự kiện thanh toán gửi trùng.

---

## 9. Người 4 — Hưng: thanh toán và tính phí giao hàng

Hưng làm trọn luồng cho các việc sau:

- Màn hình chọn cách thanh toán, chuyển sang VNPay, kết quả thanh toán và lỗi/thử lại.
- Thanh toán khi nhận hàng bằng tiền mặt (COD) và thanh toán qua VNPay ở môi trường thử nghiệm.
- Kiểm tra chữ ký, mã tham chiếu, số tiền và thông báo kết quả thanh toán từ VNPay; ghi lịch sử mọi lần nhận thông báo, kể cả sai hoặc trùng.
- Cung cấp danh mục Tỉnh/Thành phố và Phường/Xã từ GHN để dùng trong sổ địa chỉ và xác nhận đặt hàng.
- Tính phí giao hàng trên máy chủ từ địa chỉ, hàng hóa và kho gửi cố định; trả phí, dịch vụ giao và thời gian dự kiến. Không tạo vận đơn hoặc theo dõi GHN trong phạm vi hiện tại.
- Chỉ chấp nhận: trả trước bằng VNPay, trả sau bằng VNPay hoặc COD, và đơn 0 đồng. Mọi đường dẫn thanh toán VNPay có hạn 15 phút.

Kiểm thử bắt buộc: chữ ký sai, sai số tiền, sai mã tham chiếu, thông báo trùng, đường dẫn hết hạn, ghi nhận COD, lỗi hoặc hết thời gian chờ báo giá GHN và địa chỉ GHN không hợp lệ.

---

### Nguồn trạng thái đã chốt

Mục 6 trong [danh sách chức năng](10_DANH_SACH_CHUC_NANG.md) là nguồn duy nhất về trạng thái phiên đặt hàng, đơn hàng, thanh toán và giao hàng. Tóm tắt: customer xác nhận nhận hàng mới tạo `DELIVERED`; mọi Order chỉ `COMPLETED` khi đã giao và đã đạt điều kiện thanh toán; `PaymentDue` do Hiếu phát, còn Hưng chỉ phát `PaymentSucceeded`/`PaymentFailed`/`PaymentExpired` theo đúng loại thanh toán.

---

### Trình tự chọn địa chỉ và tính phí giao hàng

1. Thảo phụ trách giao diện chọn/lưu địa chỉ. Thành phần này gọi API địa giới GHN qua cổng API của hệ thống để lấy Tỉnh/Thành phố và Phường/Xã.
2. Địa chỉ được lưu hoặc chọn xong mới có `addressId` hợp lệ. Hiếu dùng mã này khi xác nhận đặt hàng.
3. Hiếu yêu cầu Hưng báo giá GHN sau bước chọn địa chỉ; đổi địa chỉ, hàng hóa hoặc mã giảm phí giao hàng thì báo giá cũ không còn dùng được.
4. Hưng chỉ sở hữu dữ liệu địa giới và báo giá ở máy chủ. Trình duyệt không gọi thẳng GHN để tránh lộ thông tin cấu hình và để phí giao hàng luôn do máy chủ quyết định.

---

## 10. Người 5 — Tùng: tương tác khách hàng và báo cáo

Tùng làm trọn luồng cho các việc sau:

- Đánh giá sản phẩm: chỉ khách đã mua và hoàn tất đơn mới được đánh giá một lần cho mỗi dòng hàng; quản trị viên có thể ẩn đánh giá vi phạm.
- Hỏi đáp sản phẩm, danh sách yêu thích và thông báo trong hệ thống là P1.
- Báo cáo doanh thu và sản phẩm bán chạy từ đơn đã hoàn tất. Doanh thu P0 là tổng tiền cuối cùng của đơn hoàn tất, chưa phải lợi nhuận và chưa trừ hoàn tiền.
- Trò chuyện giữa khách hàng và quản trị viên là P2, không phải trò chuyện với người bán.
- Nhận sự kiện đơn hàng để cập nhật báo cáo; một sự kiện gửi trùng không được làm tăng doanh thu, số bán hoặc gửi thông báo lần hai.

Kiểm thử bắt buộc: điều kiện được đánh giá; đánh giá trùng; đơn hủy không cộng doanh thu; sự kiện đơn hoàn tất trùng; và tính đúng sản phẩm bán chạy.

---

## 11. Phần làm chung

Không thuộc riêng một người, cả nhóm cùng chốt và tích hợp:

- Cách chia dịch vụ, cơ sở dữ liệu và quyền sở hữu dữ liệu.
- Mã định danh, hợp đồng gọi giữa các dịch vụ và nội dung sự kiện.
- Cổng API, hàng đợi RabbitMQ (hệ thống chuyển sự kiện), lưu sự kiện chờ gửi cùng giao dịch và cơ chế chống xử lý sự kiện trùng.
- Cấu hình chạy bằng Docker, quy tắc làm việc Git, nền giao diện dùng chung, kiểm thử tích hợp, kiểm thử toàn luồng và kịch bản trình diễn.

---

## 12. Làm song song bằng dữ liệu giả

Không chờ người khác làm xong mới bắt đầu. Mỗi người cung cấp dữ liệu giả theo đúng cấu trúc đã thống nhất, sau đó thay lần lượt bằng kết nối thật.

- Tiến cung cấp dữ liệu sản phẩm và biến thể có thể mua.
- Thảo cung cấp dữ liệu giỏ hàng, địa chỉ và mã giảm giá.
- Hiếu cung cấp dữ liệu đơn hàng cần thanh toán và các sự kiện thay đổi đơn.
- Hưng cung cấp kết quả thanh toán và báo giá GHN.
- Tùng dùng sự kiện đơn hoàn tất để làm báo cáo.

## 13. Kế hoạch hoàn thiện giao diện và điểm cần kết nối

Nguyên tắc: phần giao diện, kiểm tra dữ liệu, trạng thái đang tải/trống/lỗi và dữ liệu giả đều làm ngay. Phần “cần kết nối” chỉ là thay dữ liệu giả bằng dữ liệu thật.

| Người phụ trách | Các trang cần hoàn thiện | Làm ngay | Cần kết nối thật |
|---|---|---|---|
| Phần nền chung | Đăng ký/đăng nhập/quên mật khẩu, Gateway security và API auth contract | API auth, refresh cookie, JWT/Gateway bằng test hoặc Postman | CSDL Identity/Flyway và frontend Auth của Tiến nối theo contract |
| Người 1 — Tiến | Danh sách/chi tiết sản phẩm, bộ lọc, quản trị danh mục/sản phẩm/tồn kho, hiển thị direct sale | Bố cục trang, chọn biến thể, giá gốc/sale, trạng thái hết hàng, nút thêm giỏ và Mua ngay | Dữ liệu sản phẩm cho Thảo/Hiếu; direct sale contract từ Thảo; contract xác thực nền chung; hiển thị đánh giá từ Tùng |
| Người 2 — Thảo | Hồ sơ, sổ địa chỉ, giỏ hàng, direct sale, ví voucher cá nhân/mặc định, quản trị người dùng/khuyến mãi | Biểu mẫu, quyền sở hữu, giỏ/direct sale/voucher bằng dữ liệu giả; địa chỉ chỉ nhập số nhà/đường | Danh mục GHN từ Hưng; Variant/giá niêm yết từ Tiến; địa chỉ và lựa chọn đặt hàng từ Hiếu |
| Người 3 — Hiếu | Xác nhận đặt hàng từ giỏ/Mua ngay, chọn địa chỉ, lịch sử/chi tiết đơn, khách xác nhận đã nhận hàng, quản trị xử lý đơn | Toàn bộ màn đặt hàng bằng dữ liệu giả; thêm địa chỉ ngay trong trang; chặn gửi đơn trùng | Giỏ, direct sale, voucher, địa chỉ từ Thảo; kiểm tra/giữ hàng từ Tiến; phí giao và thanh toán từ Hưng |
| Người 4 — Hưng | Chọn thanh toán, kết quả/lỗi VNPay, hiển thị phí giao hàng | Giao diện thanh toán, danh mục GHN và báo giá bằng dữ liệu giả | Dữ liệu đơn cần thanh toán từ Hiếu; thông tin cấu hình GHN/VNPay khi triển khai |
| Người 5 — Tùng | Đánh giá, yêu thích P1, hỏi đáp P1, thông báo P1, quản trị nội dung/báo cáo, trò chuyện P2 | Bố cục, hội thoại giả ở P2, bảng báo cáo bằng dữ liệu giả | Đơn hoàn tất/đủ điều kiện đánh giá từ Hiếu; tóm tắt sản phẩm từ Tiến; thanh toán thành công từ Hưng |

### Luồng địa chỉ bắt buộc trên giao diện

1. Khách đã đăng nhập thấy các địa chỉ đã lưu và chọn một địa chỉ giao hàng.
2. Nếu thêm địa chỉ mới, khách chỉ nhập người nhận, điện thoại, số nhà/đường; Tỉnh/Thành phố và Phường/Xã bắt buộc chọn từ danh mục GHN.
3. Lưu thành công thì địa chỉ mới được chọn ngay cho lần đặt hàng này và vẫn có trong sổ địa chỉ.
4. Đổi địa chỉ sẽ làm báo giá cũ không còn hiệu lực; hệ thống phải báo giá GHN lại trước khi tạo đơn.

---

## 14. Những điều phải chốt sớm
```text
Ranh giới từng dịch vụ
Quyền sở hữu cơ sở dữ liệu
Đối tượng dữ liệu chính và mã định danh
Cấu trúc dữ liệu gọi giữa các dịch vụ và sự kiện
Trạng thái đơn hàng và thanh toán
Loại, phạm vi, trạng thái và quy tắc kết hợp mã giảm giá
Kiểu dữ liệu tiền
```

Có thể hoàn thiện sau:
```text
Chỉ mục tối ưu
Dữ liệu mẫu đầy đủ
Ràng buộc phụ
Tối ưu truy vấn
Trường ghi lịch sử bổ sung
```

---

## 15. Tiêu chí hoàn thành một phần việc

Một phần việc chỉ hoàn thành khi:

- Giao diện và phần xử lý máy chủ chạy được.
- Các điểm gọi giữa hệ thống đã được mô tả.
- Quy tắc nghiệp vụ và kiểm thử đều đạt.
- Dữ liệu giả đã được thay bằng kết nối thật.
- Có kịch bản trình diễn và tài liệu.
