# Thanh toán và xử lý lỗi tại Fashion Ecommerce

Fashion Ecommerce có các tích hợp thanh toán như COD, QR, VNPay, ZaloPay, AlePay, payOS, PayPal, Google Pay qua PayPal và ShopeePay. Tuy nhiên, khách chỉ nên chọn trong danh sách thực tế đang được hiển thị tại checkout hoặc trang thanh toán của đơn; một tích hợp có trong hệ thống không có nghĩa là luôn được bật cho mọi môi trường hoặc mọi đơn hàng.

Trạng thái thanh toán có thể là Chờ thanh toán, Thanh toán thành công, Thanh toán thất bại, Thanh toán bị hủy hoặc Thanh toán hết hạn. Trang trình duyệt quay về sau khi thanh toán không phải là bằng chứng duy nhất của giao dịch thành công. Máy chủ còn phải kiểm tra dữ liệu từ nhà cung cấp, chữ ký, mã đơn, số tiền và việc callback có bị gửi lặp hay không.

Nếu thanh toán trực tuyến thất bại hoặc hết hạn, khách mở chi tiết đơn hoặc trang thanh toán để xem hệ thống có cung cấp thao tác thử lại hay không. Không tạo nhiều đơn liên tiếp chỉ vì trình duyệt tải chậm. Không chuyển tiền lần nữa nếu chưa kiểm tra trạng thái đơn và trạng thái thanh toán.

Với COD, khách thanh toán theo hướng dẫn khi nhận hàng. Số tiền cuối cùng phải lấy từ chi tiết đơn đã được máy chủ tạo, không lấy từ phép tính do chatbot đưa ra.

Nếu đã bị trừ tiền nhưng đơn chưa ghi nhận thành công, khách không gửi số thẻ, mã OTP hoặc thông tin đăng nhập cho chatbot. Hãy lưu mã đơn, mã giao dịch an toàn và liên hệ kênh hỗ trợ để nhân viên đối soát.
