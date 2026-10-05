# Thanh toán và giao hàng tại DynamicMart

DynamicMart hỗ trợ trả trước bằng VNPay, trả sau bằng VNPay, thanh toán khi nhận hàng bằng COD và đơn hàng không đồng. Danh sách thực tế cho một đơn được hiển thị tại bước thanh toán.

Đường dẫn thanh toán VNPay và phiên checkout có hạn 15 phút. Kết quả quay lại từ trình duyệt không tự chứng minh giao dịch thành công. Máy chủ kiểm tra chữ ký, mã tham chiếu, số tiền, trạng thái hiện tại và mã giao dịch; callback gửi lặp chỉ được xử lý một lần.

Phí giao hàng được máy chủ báo giá qua GHN từ một kho gửi cố định. Khách chọn Tỉnh/Thành phố và Phường/Xã từ danh mục GHN, sau đó nhập số nhà và đường. Đổi địa chỉ, hàng hóa hoặc voucher giảm phí giao hàng sẽ làm báo giá cũ không còn hợp lệ.

Phạm vi hiện tại dùng GHN để lấy địa giới và báo giá, chưa tạo vận đơn hoặc theo dõi giao hàng thực tế qua GHN.

