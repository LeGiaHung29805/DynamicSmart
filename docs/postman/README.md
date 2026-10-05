# Postman: Payment & Shipping

## Chuẩn bị

1. Import `DynamicMart-Payment-Shipping.postman_collection.json`.
2. Import `DynamicMart-Payment-Shipping.postman_environment.json` và chọn environment **DynamicMart Local**.
3. Mở `services/payment-service/.env`, chép giá trị `INTERNAL_API_KEY` vào biến secret `internalApiKey`.
4. Để chạy callback VNPay thành công, chép `VNPAY_HASH_SECRET` vào biến secret `vnpayHashSecret`. Không commit hai secret này vào Git.
5. Đảm bảo frontend, Gateway và các service local đang chạy. Collection gọi Gateway tại `http://localhost:18080`.

## Cách chạy

Chạy từng folder theo thứ tự **01 → 06**. Không nên chạy lại riêng request **Dùng báo giá đúng fingerprint**, vì quote chỉ được dùng một lần. Muốn chạy lại folder 03, hãy chạy lại từ request **Tạo báo giá GHN**.

Collection sử dụng dữ liệu seed:

- Customer: `customer@dynamicmart.local` / `Password@123`
- Admin: `admin@dynamicmart.local` / `Password@123`
- Customer ID: `00000000-0000-4000-8000-000000000002`
- Variant: `50000000-0000-0000-0000-000000000001`
- Hà Nội / Phường Phú Diễn: `201` / `11007`

Folder 04 và 05 tự sinh Order ID/Correlation ID mới nên có thể chạy lại. Folder 05 giả lập IPN VNPay có chữ ký thật bằng `VNPAY_HASH_SECRET`; không cần mở trang sandbox để hoàn tất callback.

Nếu request tạo quote trả `GHN_NOT_CONFIGURED`, kiểm tra `GHN_TOKEN`, `GHN_SHOP_ID`, kho gửi và khởi động lại Payment Service. Nếu tạo VNPay trả `VNPAY_NOT_CONFIGURED`, kiểm tra nhóm biến `VNPAY_*`.
