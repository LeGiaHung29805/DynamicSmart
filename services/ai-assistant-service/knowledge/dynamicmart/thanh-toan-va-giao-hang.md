# Thanh toán và giao hàng tại DynamicMart

DynamicMart hỗ trợ thanh toán khi nhận hàng bằng COD và các phương thức thanh toán trực tuyến mà hệ thống hiển thị tại checkout. Hạ tầng Payment hiện có thể tạo lần thanh toán cho VNPay, ZaloPay, PayOS hoặc QR ngân hàng; phương thức thực tế khả dụng phụ thuộc cấu hình môi trường và trạng thái của từng đơn. Đơn có tổng thanh toán 0 đồng không yêu cầu khách nhập thông tin thanh toán. Danh sách quyết định cho một đơn luôn là danh sách được máy chủ hiển thị tại bước thanh toán.

Đường dẫn thanh toán VNPay và phiên checkout có hạn 15 phút. Kết quả quay lại từ trình duyệt không tự chứng minh giao dịch thành công. Máy chủ kiểm tra chữ ký, mã tham chiếu, số tiền, trạng thái hiện tại và mã giao dịch; callback gửi lặp chỉ được xử lý một lần.

Nếu lần thanh toán trực tuyến hết hiệu lực hoặc chưa thành công, khách mở chi tiết đơn hàng để xem trạng thái authoritative và tạo lại đường dẫn hoặc mã thanh toán khi hệ thống cho phép. Không chuyển tiền theo đường dẫn do chatbot hoặc người lạ cung cấp. Trường hợp tài khoản đã bị trừ tiền nhưng đơn chưa ghi nhận, khách không thanh toán lặp ngay mà liên hệ hỗ trợ và cung cấp mã đơn hoặc mã giao dịch qua kênh đã xác thực.

Với COD, việc thu tiền được ghi nhận theo quy trình giao nhận và trạng thái đơn. Chatbot không tự xác nhận đã thu COD, không đổi trạng thái Payment và không yêu cầu khách gửi ảnh thẻ hay thông tin ngân hàng nhạy cảm.

Phí giao hàng được máy chủ báo giá qua GHN từ một kho gửi cố định. Khách chọn Tỉnh/Thành phố và Phường/Xã từ danh mục GHN, sau đó nhập số nhà và đường. Đổi địa chỉ, hàng hóa hoặc voucher giảm phí giao hàng sẽ làm báo giá cũ không còn hợp lệ.

Phạm vi hiện tại dùng GHN để lấy địa giới và báo giá, chưa tạo vận đơn hoặc theo dõi giao hàng thực tế qua GHN.

