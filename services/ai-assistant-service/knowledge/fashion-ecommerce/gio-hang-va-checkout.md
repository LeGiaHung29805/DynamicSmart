# Giỏ hàng và checkout tại Fashion Ecommerce

Mỗi dòng trong giỏ hàng tương ứng với một biến thể sản phẩm, chẳng hạn một tổ hợp kích thước và màu sắc. Khách cần chọn đúng biến thể trước khi thêm sản phẩm. Giá và tồn kho được lấy từ biến thể, không lấy từ dữ liệu do trình duyệt hoặc chatbot tự tính.

Khách có thể thay đổi số lượng, xóa mặt hàng và chọn những dòng muốn thanh toán. Chỉ các dòng được chọn và còn hợp lệ mới được đưa vào checkout. Nếu sản phẩm bị ngừng bán, hết hàng hoặc số lượng yêu cầu vượt tồn kho, khách phải điều chỉnh giỏ trước khi tiếp tục.

Khách chưa đăng nhập có thể lưu giỏ trong phiên trình duyệt, nhưng phải đăng nhập trước khi hoàn tất đặt hàng. Sau đăng nhập, hệ thống gộp các mặt hàng hợp lệ và cảnh báo nếu một biến thể không còn bán hoặc số lượng phải giảm theo tồn kho.

Chức năng Mua ngay tạo lựa chọn checkout riêng cho biến thể và số lượng vừa chọn. Luồng này không được tự ý xóa hoặc thay đổi các mặt hàng khác đang có trong giỏ.

Tại checkout, máy chủ tải lại biến thể, giá, tồn kho, voucher và phí giao hàng. Việc giỏ hàng từng hiển thị một mức giá hoặc tồn kho không bảo đảm giá trị đó vẫn giữ nguyên đến lúc tạo đơn.

Giỏ chỉ được dọn sau khi đơn hàng được tạo thành công. Nếu checkout thất bại, hệ thống không được tạo đơn một phần hoặc làm mất giỏ của khách.
