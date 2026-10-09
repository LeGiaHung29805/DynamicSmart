# Đánh giá, sản phẩm yêu thích và hỏi đáp tại DynamicMart

Đánh giá công khai của sản phẩm chỉ hiển thị nội dung đang ở trạng thái được phép hiển thị. Khách muốn tạo đánh giá phải đăng nhập và chọn một dòng sản phẩm trong đơn hàng đủ điều kiện; máy chủ tự kiểm tra quyền đánh giá từ Order Service, không tin product ID hoặc variant ID do trình duyệt tự khai báo. Đánh giá có thể gồm số sao, nội dung và các URL ảnh hợp lệ.

Mỗi khách có một danh sách sản phẩm yêu thích. Khách có thể xem danh sách, thêm sản phẩm hoặc bỏ sản phẩm; cùng một sản phẩm không xuất hiện trùng trong wishlist của một tài khoản. Wishlist không giữ giá hoặc tồn kho, vì vậy khách vẫn phải xem lại chi tiết sản phẩm trước khi mua.

Hỏi đáp sản phẩm cho phép mọi người xem các câu hỏi được công khai theo sản phẩm. Khách đã đăng nhập có thể gửi câu hỏi và xem câu hỏi của mình. Câu hỏi có thể ở trạng thái đang chờ trả lời, đã trả lời hoặc bị ẩn; câu trả lời chính thức do quản trị viên cung cấp.

Chatbot có thể giải thích cách sử dụng đánh giá, wishlist và hỏi đáp, nhưng không tự tạo đánh giá, không trả lời thay quản trị viên và không tiết lộ nội dung đã bị ẩn. Khi hỏi về chất lượng một sản phẩm cụ thể, dữ liệu đánh giá hiện tại trên trang sản phẩm có giá trị hơn suy đoán của chatbot.
