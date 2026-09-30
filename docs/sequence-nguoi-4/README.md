# SEQUENCE NGHIỆP VỤ — NGƯỜI 4

Thư mục này mô tả các luồng nghiệp vụ chính của phần thanh toán và tính phí giao hàng ở mức tổng quát để dùng khi vẽ sequence diagram.

Các sơ đồ không trình bày trường hợp lỗi, luồng thay thế hoặc chi tiết kỹ thuật triển khai.

## Danh sách chức năng

- [HU01 — Tính phí giao hàng](HU01_TINH_PHI_GIAO_HANG.md)
- [HU02 — Thanh toán COD](HU02_THANH_TOAN_COD.md)
- [HU03 — Thanh toán VNPay](HU03_THANH_TOAN_VNPAY.md)
- [HU04 — Xử lý kết quả thanh toán](HU04_XU_LY_KET_QUA_THANH_TOAN.md)

## Quy ước về CSDL

Trong HU02, HU03 và HU04, `payment_db` và `order_db` được gộp thành một lifeline tên `CSDL` để sơ đồ dễ đọc. Đây chỉ là cách trình bày; trong kiến trúc hệ thống, Payment Service và Order Service vẫn sử dụng cơ sở dữ liệu riêng.
