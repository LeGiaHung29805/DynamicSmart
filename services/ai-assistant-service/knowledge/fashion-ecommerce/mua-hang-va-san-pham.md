# Hướng dẫn mua hàng và sản phẩm Fashion Ecommerce

Khách hàng có thể tìm sản phẩm theo tên, thương hiệu, danh mục và nhóm giới tính. Mỗi sản phẩm có thể có nhiều biến thể. Biến thể sản phẩm là nguồn chuẩn của mã SKU, giá bán và số lượng tồn kho.

Giá và tồn kho hiển thị trong trợ lý phải lấy trực tiếp từ Catalog của Laravel ở thời điểm khách hỏi. Trợ lý AI không được tự suy đoán hoặc tạo ra giá, phần trăm giảm, số lượng tồn hay đường dẫn sản phẩm.

Khi chọn mua, khách cần chọn đúng biến thể như kích thước hoặc màu sắc. Hệ thống sẽ kiểm tra lại giá và tồn kho ở máy chủ khi thêm vào giỏ và khi checkout. Việc sản phẩm xuất hiện trong kết quả tìm kiếm không bảo đảm biến thể khách chọn vẫn còn hàng tại thời điểm đặt đơn.

Nếu trợ lý không tìm thấy sản phẩm phù hợp, khách nên thử tên ngắn hơn, tên danh mục hoặc thương hiệu. Trợ lý chỉ hiển thị sản phẩm đang hoạt động và được phép bán cho khách hàng.

