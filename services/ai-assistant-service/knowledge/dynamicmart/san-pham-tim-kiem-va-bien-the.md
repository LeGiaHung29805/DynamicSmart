# Sản phẩm, tìm kiếm và biến thể tại DynamicMart

DynamicMart là hệ thống thương mại điện tử B2C đa ngành hàng do một doanh nghiệp bán trực tiếp. Đây không phải marketplace nhiều người bán và không có quy trình tách đơn hoặc thanh toán cho từng người bán.

Khách có thể tìm sản phẩm bằng từ khóa, duyệt danh mục, lọc theo khoảng giá, thuộc tính và sản phẩm nổi bật, sau đó sắp xếp kết quả theo các lựa chọn giao diện cung cấp. Chỉ sản phẩm và danh mục đang hoạt động, đã được công khai mới được hiển thị cho khách.

Một sản phẩm có thể có nhiều biến thể. Biến thể là đơn vị thực tế được bán và có mã SKU, giá, tồn kho cùng các thuộc tính riêng. Khách phải chọn đúng biến thể trước khi thêm vào giỏ hoặc dùng Mua ngay.

Giá sale và phần trăm giảm, nếu có, phải do dịch vụ khuyến mãi cung cấp. Frontend và chatbot không tự tính giá sale. Giá, tồn kho và khả năng mua cần được kiểm tra lại ở máy chủ khi xem giỏ, tạo phiên checkout và tạo đơn.

Chatbot RAG không tự đọc catalog database. Khi cần tư vấn sản phẩm cụ thể, API của DynamicMart phải cung cấp dữ liệu catalog hiện tại cho chatbot. Nếu không có dữ liệu sản phẩm đáng tin cậy, chatbot chỉ hướng dẫn cách tìm kiếm và không được tự tạo tên, giá, tồn kho hay đường dẫn sản phẩm.
