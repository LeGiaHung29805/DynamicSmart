# Mua hàng và checkout tại DynamicMart

DynamicMart là hệ thống thương mại điện tử B2C do một doanh nghiệp bán trực tiếp. Sản phẩm có biến thể; biến thể là đơn vị có thể bán, mang SKU, giá, tồn kho và thuộc tính riêng.

Khách có thể mua từ giỏ hàng hoặc dùng Mua ngay. Mua ngay dùng cùng luồng đặt hàng với giỏ nhưng chỉ chứa biến thể và số lượng vừa chọn, không tự thay đổi giỏ hàng. Trước khi tạo đơn, máy chủ kiểm tra lại địa chỉ, sản phẩm, giá, tồn kho, voucher và phí giao hàng.

Chỉ các dòng giỏ đã mua thành công mới được dọn. Thay đổi mới trong giỏ không được xóa nhầm. Mỗi phiên checkout chỉ tạo một đơn ngay cả khi khách gửi lại yêu cầu.

Giá, tổng tiền, tồn kho và trạng thái đơn phải lấy từ các dịch vụ nghiệp vụ của DynamicMart. Chatbot không tự tính hoặc xác nhận các giá trị này.

