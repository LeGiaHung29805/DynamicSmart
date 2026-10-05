# Phiên checkout và tạo đơn tại DynamicMart

DynamicMart dùng phiên checkout để chốt lựa chọn mua trước khi tạo đơn. Phiên có thể được tạo từ giỏ hàng hoặc từ Mua ngay. Mua ngay chỉ chứa biến thể và số lượng vừa chọn, không tự thay đổi giỏ hiện tại.

Phiên checkout có thời hạn 15 phút. Trạng thái phiên gồm đang hoạt động, đã hoàn tất, đã hủy và đã hết hạn. Khi địa chỉ, mặt hàng, số lượng hoặc voucher thay đổi, hệ thống phải kiểm tra lại thông tin liên quan; báo giá vận chuyển cũ có thể không còn hợp lệ.

Trước khi tạo đơn, máy chủ kiểm tra lại khách hàng, địa chỉ, biến thể, giá, tồn kho, voucher và phí giao hàng. Tồn kho và voucher chỉ được giữ trong chuỗi tạo đơn, không bị giữ chỉ vì khách xem trang preview.

Quy trình tạo đơn có chống gửi lặp để một phiên checkout không tạo nhiều đơn. Nếu một bước giữ tồn, giữ voucher hoặc tạo đơn bị lỗi, hệ thống thực hiện bù trừ những phần đã giữ. Chatbot không thể bỏ qua quy trình này và không tự tạo đơn thay cho API nghiệp vụ.

Sau khi đơn được xác nhận, hệ thống chỉ dọn những dòng giỏ thực sự đã mua và không xóa nhầm thay đổi mới mà khách thực hiện sau lúc mở checkout.
