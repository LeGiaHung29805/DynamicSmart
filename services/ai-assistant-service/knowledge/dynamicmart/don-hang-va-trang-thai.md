# Đơn hàng và trạng thái tại DynamicMart

Các trạng thái đơn hàng của DynamicMart gồm: chờ thanh toán, đã xác nhận, đang đóng gói, đang giao hàng, chờ bàn giao, đã giao, hoàn tất và đã hủy. Trạng thái được chuyển theo state machine của Order Service, không thay đổi tùy ý từ frontend hoặc chatbot.

Đơn trả trước có thể bắt đầu ở trạng thái chờ thanh toán. Khi thanh toán hợp lệ được xác nhận, đơn chuyển sang đã xác nhận. Nếu thanh toán trả trước thất bại hoặc hết hạn khi đơn vẫn chờ thanh toán, hệ thống có thể hủy đơn theo quy trình.

Đơn đã xác nhận mới được chuyển sang đóng gói; đơn đang đóng gói mới chuyển sang giao hàng. Với thanh toán trả sau, trạng thái chờ bàn giao được dùng trước khi hoàn tất việc nhận hàng và thanh toán theo quy trình.

Thông tin sản phẩm, giá, voucher và vận chuyển của đơn được lưu dưới dạng bản chụp để lịch sử không thay đổi theo catalog hiện tại. Khách phải đăng nhập và sử dụng màn hình đơn hàng để xem dữ liệu của một đơn cụ thể.

Chatbot chỉ giải thích ý nghĩa trạng thái. Chatbot công khai không được xác nhận một mã đơn thuộc về ai, không tự hủy đơn, không đánh dấu đã nhận hàng và không đọc lịch sử trạng thái cá nhân.
