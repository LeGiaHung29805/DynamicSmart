# Cấu hình cổng thanh toán DynamicMart

DynamicMart hỗ trợ `COD`, `VNPAY`, `ZALOPAY`, `PAYOS` và `BANK_QR` (SePay đối soát). GHTK là dịch vụ vận chuyển, không phải phương thức thanh toán.

## URL public

| Cổng | Callback/webhook tại Gateway | Return/cancel tại Frontend |
|---|---|---|
| VNPay | `/api/v1/payments/vnpay/ipn` | `/checkout/payment/result` |
| ZaloPay | `/api/v1/payments/zalopay/callback` | `/checkout/payment/result` |
| PayOS | `/api/v1/payments/payos/webhook` | `/checkout/payment/result` |
| SePay | `/api/v1/payments/sepay/webhook` | Không dùng return URL; UI polling trạng thái Payment. |

Nếu dùng một domain ngrok cho cả Gateway và Frontend thì phải có reverse proxy: `/api/**` chuyển tới Gateway, các đường dẫn còn lại chuyển tới Frontend. Nếu không có reverse proxy, dùng hai public domain riêng.

## Biến môi trường

Các biến và placeholder đầy đủ nằm tại `services/payment-service/.env.example`. Secret thật chỉ đặt trong `services/payment-service/.env`, không commit.

- VNPay: `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_PAYMENT_URL`, `VNPAY_RETURN_URL`.
- ZaloPay: `ZALOPAY_APP_ID`, `ZALOPAY_KEY1`, `ZALOPAY_KEY2`, `ZALOPAY_CREATE_URL`, `ZALOPAY_CALLBACK_URL`, `ZALOPAY_RETURN_URL`.
- PayOS: `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`, `PAYOS_BASE_URL`, `PAYOS_RETURN_URL`, `PAYOS_CANCEL_URL`.
- SePay/VietQR: `SEPAY_WEBHOOK_SECRET`, `PAYMENT_QR_BANK_ID`, `PAYMENT_QR_ACCOUNT_NO`, `PAYMENT_QR_ACCOUNT_NAME`, `PAYMENT_QR_BASE_URL`.

## Quy tắc xác nhận thanh toán

- Return URL không cập nhật Payment.
- VNPay kiểm tra secure hash; ZaloPay kiểm tra HMAC-SHA256 bằng Key2; PayOS kiểm tra checksum; SePay kiểm tra `X-SePay-Signature`, `X-SePay-Timestamp`, raw body, mã chuyển khoản và số tiền.
- Callback lặp chỉ tạo một `PaymentSucceeded`.
- Callback sai chữ ký, sai reference hoặc sai số tiền vẫn được audit nhưng không đổi trạng thái Payment.

Tài liệu nhà cung cấp: [ZaloPay callback](https://docs.zalopay.vn/docs/developer-tools/knowledge-base/callback/), [PayOS API](https://payos.vn/docs/api/), [SePay webhook authentication](https://developer.sepay.vn/en/sepay-webhooks/xac-thuc).
