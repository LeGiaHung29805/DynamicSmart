# Giỏ hàng và voucher tại DynamicMart

Mỗi dòng giỏ hàng lưu biến thể sản phẩm và số lượng. Giá do client gửi lên không phải nguồn quyết định. Hệ thống lấy lại thông tin biến thể và kiểm tra khả năng mua trước khi báo giá hoặc tạo đơn.

Trong phạm vi P0, khách phải đăng nhập để đặt hàng và xem voucher cá nhân hoặc voucher mặc định. Giỏ khách chưa đăng nhập và gộp giỏ sau đăng nhập thuộc phạm vi mở rộng P1, vì vậy chatbot không nên khẳng định chức năng đó đã khả dụng nếu giao diện hiện tại chưa hiển thị.

Voucher có thể giới hạn theo thời gian hiệu lực, trạng thái, giá trị đơn tối thiểu, tổng tiền đủ điều kiện, sản phẩm, danh mục, số lượt toàn hệ thống và số lượt của từng khách. Voucher giảm theo số tiền cố định hoặc tỷ lệ; mức giảm tối đa có thể được áp dụng.

Kết quả xem trước voucher chỉ dùng để khách kiểm tra trước. Khi tạo đơn, máy chủ tính lại quyền sử dụng và số tiền giảm. Voucher được giữ theo phiên checkout và được ghi nhận đã dùng khi đơn được xác nhận. Nếu chuỗi tạo đơn thất bại, phần giữ voucher phải được giải phóng theo quy trình bù trừ.

Khách không nên dựa vào số tiền giảm do chatbot tự tính. Kết quả cuối cùng là giá trị được hiển thị bởi checkout sau khi máy chủ kiểm tra sản phẩm, khách hàng và voucher.
