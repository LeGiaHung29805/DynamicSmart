# Đơn hàng và trạng thái tại Fashion Ecommerce

Khách đã đăng nhập xem danh sách đơn tại mục lịch sử đơn hàng và mở chi tiết bằng mã đơn. Chatbot công khai không tra cứu trạng thái của một mã đơn cụ thể và không tiết lộ dữ liệu đơn hàng.

Các trạng thái đơn hàng gồm: Chờ xử lý, Đang xử lý, Đã đóng gói, Đang giao hàng, Đã giao hàng, Đã nhận hàng và Đã hủy. Trạng thái chỉ được thay đổi theo quy trình của hệ thống; khách hoặc quản trị viên không thể bỏ qua tùy ý các bước bắt buộc.

Khách chỉ có thể tự hủy khi đơn thuộc tài khoản của mình, đang ở trạng thái Chờ xử lý, chưa có vận chuyển và chưa có thanh toán thành công. Đơn đang xử lý, đã đóng gói, đang giao, đã giao, đã nhận hoặc đã hủy không thể tự hủy bằng thao tác này. Nếu nút hủy không xuất hiện, khách sử dụng kênh hỗ trợ để được kiểm tra; chatbot không tự hủy đơn.

Khi đơn đã được giao và giao diện cho phép, khách có thể xác nhận đã nhận hàng. Nếu đơn có thông tin vận chuyển, trang chi tiết đơn có thể hiển thị thao tác theo dõi. Việc có mã đơn không đủ để chatbot công khai xác nhận chủ sở hữu.

Thông tin tên sản phẩm, SKU, tùy chọn, đơn giá và số lượng trên đơn được lưu dưới dạng bản chụp tại thời điểm mua. Vì vậy lịch sử đơn không phụ thuộc vào việc sản phẩm hiện tại đã đổi tên, đổi giá hoặc ngừng bán.
