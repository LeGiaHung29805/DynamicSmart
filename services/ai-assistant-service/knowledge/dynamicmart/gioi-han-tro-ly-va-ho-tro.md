# Giới hạn trợ lý và cách nhận hỗ trợ tại DynamicMart

## Bạn là ai?

Tôi là trợ lý AI của DynamicMart và bạn có thể trò chuyện với tôi để tìm sản phẩm hoặc hỏi cách sử dụng hệ thống. Tôi không phải nhân viên đang trực tiếp xử lý đơn hàng và không thay thế khu vực tài khoản có xác thực.

## Bạn có thể làm gì?

Bạn có thể yêu cầu trợ lý tìm và gợi ý sản phẩm theo nhu cầu, cách sử dụng, danh mục hoặc khoảng giá. Kết quả có thể hiển thị sản phẩm đang bán cùng giá, khuyến mãi và tình trạng còn hàng do Catalog Service cung cấp.

Trợ lý giúp bạn tìm sản phẩm và giải đáp cách sử dụng DynamicMart; trợ lý không trực tiếp thao tác trên tài khoản hoặc đơn hàng. Nếu bạn chưa biết nên hỏi gì, bạn có thể hỏi về sản phẩm, giỏ hàng, voucher, checkout, thanh toán, giao hàng, trạng thái đơn hoặc cách liên hệ hỗ trợ.

Bạn có thể hỏi cách tìm sản phẩm, chọn biến thể, sử dụng giỏ hàng, voucher và quy trình checkout. Trợ lý cũng giải thích thông tin chung về đơn hàng, thanh toán, giao hàng, tài khoản, địa chỉ, wishlist, đánh giá, hỏi đáp, thông báo và hỗ trợ khách hàng.

Bạn có thể hỏi nối tiếp trong phiên trò chuyện, chẳng hạn “mẫu đầu tiên thì sao” hoặc “loại đó còn hàng không”.

Khách có thể bắt đầu bằng một yêu cầu cụ thể như “gợi ý đồ để nghe nhạc không dây”, “tìm áo dưới 500 nghìn”, “phí giao hàng được tính thế nào” hoặc “đơn hàng có những trạng thái nào”.

## Nguồn dữ liệu của trợ lý

Hướng dẫn và chính sách chung được lấy từ kho kiến thức đã kiểm duyệt. Sản phẩm được tìm bằng semantic vector search, sau đó giá, khuyến mãi và tồn kho được kiểm tra lại từ Catalog Service. Trợ lý không tự tạo tên sản phẩm, giá, tồn kho hoặc chính sách không có trong nguồn.

## Những việc trợ lý không thể thực hiện

Trợ lý không phải nguồn quyết định cho giá, tồn kho, voucher, phí vận chuyển, trạng thái thanh toán hoặc trạng thái đơn. Các giá trị thực tế phải được xác nhận bởi dịch vụ nghiệp vụ và giao diện DynamicMart.

Trợ lý không thực hiện đăng nhập, đặt hàng, thanh toán, hủy đơn, hoàn tiền, đổi trả hoặc thay đổi hồ sơ. Những thao tác này phải đi qua API và giao diện có xác thực của DynamicMart.

Không gửi mật khẩu, mã OTP, access token, refresh token, cookie, dữ liệu thẻ, khóa API hoặc chữ ký thanh toán cho chatbot. Với dữ liệu cá nhân như địa chỉ và đơn hàng, khách chỉ sử dụng màn hình tài khoản được bảo vệ.

Nếu câu hỏi không có trong nguồn, trợ lý phải nói rõ chưa đủ thông tin thay vì đoán. Các chính sách chưa được dự án công bố, như thời hạn đổi trả cụ thể, không được tự tạo thành câu trả lời.

Khách cần liên hệ hỗ trợ nhân viên khi gặp giao dịch bị trừ tiền nhưng chưa ghi nhận, cần xử lý ngoại lệ của đơn, khiếu nại giao nhận, đổi trả hoặc vấn đề tài khoản. Hãy cung cấp thông tin tối thiểu cần thiết qua kênh đã xác thực.
