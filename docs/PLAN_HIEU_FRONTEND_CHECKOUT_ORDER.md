# Kế hoạch Frontend Checkout và Order của Hiếu

## 1. Mục đích

Tài liệu này mô tả phạm vi, hiện trạng, contract, thứ tự triển khai và tiêu chí hoàn thành phần Frontend do Hiếu phụ trách.
Nội dung được lập sau khi đồng bộ nhánh `VanHieu/order-checkout-foundation` với `origin/main` tại commit `b4cd3ff`.

Phạm vi chính:

- `dynamicmart-frontend/src/features/checkout/`
- `dynamicmart-frontend/src/features/order/`
- Các route Checkout và Order trong `dynamicmart-frontend/src/app/`
- Các thay đổi tích hợp tối thiểu ở Cart, Catalog, Payment hoặc layout chung khi luồng Checkout/Order bắt buộc cần đến

Không thuộc phạm vi sở hữu trực tiếp của Hiếu:

- `features/payment/`, trang quản trị Payment và Payment callback: Hưng phụ trách.
- `features/cart/`, voucher và hồ sơ khách hàng: Thảo phụ trách.
- `features/auth/`, `features/catalog/`: Tiến phụ trách.
- `features/engagement/`, `features/reporting/`: Tùng phụ trách.

Khi cần sửa điểm tích hợp thuộc feature khác, thay đổi phải nhỏ, dựa trên contract đã có và cần owner tương ứng review.

## 2. Hiện trạng sau khi đồng bộ

### 2.1. Kiểm tra kỹ thuật

- `npm run lint`: đạt.
- `npm run build`: đạt.
- Next.js tạo được 26 trang.
- Chưa có test frontend tự động.

### 2.2. Checkout hiện có

Checkout đang được đặt trong `features/payment/components/CheckoutPaymentWizard.tsx`, dù quy ước ownership giao `checkout` cho Hiếu và `payment` cho Hưng.

Luồng hiện tại:

1. Đọc địa chỉ và voucher.
2. Gọi `POST /api/v1/cart/checkout/preview`.
3. Gọi compatibility API `POST /api/v1/checkout/orders`.
4. Dùng `Idempotency-Key` khi tạo Order.
5. Chuyển sang `redirectUrl` nếu response có URL thanh toán.

Khoảng trống:

- Chưa sử dụng Checkout Session chuẩn của Order Service.
- `Mua ngay` gọi `/api/v1/checkout-sessions/buy-now`, nhưng backend và Gateway không có endpoint này.
- `sessionId` trên URL Checkout không được đọc.
- `/checkout` và `/checkout/payment` đang render cùng một wizard.
- Thay đổi địa chỉ/voucher/payment chưa được lưu qua `PATCH /api/v1/checkout/sessions/{sessionId}`.
- Chưa xử lý rõ Checkout hết hạn, đã tạo Order, đang tạo Order hoặc replay.
- Chưa có test cho nhấn tạo Order hai lần, refresh trang hoặc quay lại bước trước.

### 2.3. Order hiện có

`origin/main` đã thêm:

- `features/order/api/customer-orders.api.ts`.
- `features/order/components/CustomerOrderDetail.tsx`.
- Route `/customer/orders/[id]`.
- Xác nhận `Đã nhận hàng` bằng `Idempotency-Key`.

Khoảng trống:

- Chưa có danh sách Order của Customer.
- Menu tài khoản trỏ đến `/customer/account/orders`, nhưng route chưa tồn tại.
- Trang chi tiết mới chỉ hiển thị thông tin tối thiểu; chưa có item, địa chỉ, tiền, voucher, vận chuyển và timeline đầy đủ.
- Chưa có API/tính năng timeline tách riêng.
- `/admin/orders` vẫn là UI mẫu, chưa gọi API thật.
- Chưa có thao tác Admin: đóng gói, giao vận và bàn giao.
- Chưa có loading/empty/error/pagination/filter hoàn chỉnh cho danh sách Order.
- Chưa có kiểm thử ownership: Customer A không xem hoặc thao tác Order của Customer B.

## 3. Contract backend authoritative

Frontend chỉ gọi qua API Gateway và không gửi giá, tồn kho, phí vận chuyển, discount hoặc trạng thái Order như nguồn dữ liệu đáng tin.

### 3.1. Checkout Session

| Mục đích | Endpoint |
|---|---|
| Tạo session từ Cart hoặc Buy Now | `POST /api/v1/checkout/sessions` |
| Tải lại session | `GET /api/v1/checkout/sessions/{sessionId}` |
| Cập nhật địa chỉ/payment selection | `PATCH /api/v1/checkout/sessions/{sessionId}` |
| Preview authoritative | `POST /api/v1/checkout/sessions/{sessionId}/preview` |
| Hủy session | `DELETE /api/v1/checkout/sessions/{sessionId}` |
| Tạo Order từ session | `POST /api/v1/checkout/sessions/{sessionId}/orders` |

Tạo session từ Cart:

```json
{
  "source": "CART",
  "cartId": "uuid"
}
```

Tạo session Buy Now:

```json
{
  "source": "BUY_NOW",
  "variantId": "uuid",
  "quantity": 1
}
```

Create Order không nhận body giá/fee/voucher. Frontend chỉ gửi `Idempotency-Key` dạng UUID; backend đọc toàn bộ snapshot từ Checkout Session.

### 3.2. Customer Order

| Mục đích | Endpoint |
|---|---|
| Danh sách đơn của Customer hiện tại | `GET /api/v1/orders` |
| Chi tiết đơn thuộc Customer hiện tại | `GET /api/v1/orders/{orderId}` |
| Timeline đã che dữ liệu nội bộ | `GET /api/v1/orders/{orderId}/timeline` |
| Xác nhận đã nhận hàng | `POST /api/v1/orders/{orderId}/received` |

Các query danh sách hỗ trợ:

- `page`, `size`
- `status`
- `orderNumber`
- `createdFrom`, `createdTo`
- `sort`

### 3.3. Admin Order

| Mục đích | Endpoint |
|---|---|
| Danh sách toàn bộ Order | `GET /api/v1/orders/admin` |
| Chi tiết Order | `GET /api/v1/orders/admin/{orderId}` |
| Timeline audit đầy đủ | `GET /api/v1/orders/admin/{orderId}/timeline` |
| Chuyển sang đóng gói | `POST /api/v1/orders/admin/{orderId}/pack` |
| Chuyển sang vận chuyển | `POST /api/v1/orders/admin/{orderId}/ship` |
| Xác nhận bàn giao | `POST /api/v1/orders/admin/{orderId}/handover` |

Mọi command phải có `Idempotency-Key` UUID mới cho thao tác mới và giữ lại cùng key khi retry cùng thao tác.

## 4. Thiết kế frontend đích

```text
src/features/checkout/
├── api/checkout.api.ts
├── components/CheckoutPage.tsx
├── components/CheckoutSteps.tsx
├── components/CheckoutSummary.tsx
├── components/CheckoutExpiredState.tsx
├── types/checkout.types.ts
└── index.ts

src/features/order/
├── api/customer-orders.api.ts
├── api/admin-orders.api.ts
├── components/CustomerOrderList.tsx
├── components/CustomerOrderDetail.tsx
├── components/AdminOrderWorkspace.tsx
├── components/OrderDetailPanel.tsx
├── components/OrderTimeline.tsx
├── components/OrderStatusBadge.tsx
├── types/order.types.ts
├── utils/order-format.ts
└── index.ts
```

Route đích:

```text
/checkout?sessionId={uuid}
/checkout/payment/result
/customer/account/orders
/customer/account/orders/{orderId}
/admin/orders
```

Route `/customer/orders/{orderId}` hiện có sẽ được giữ tạm bằng redirect hoặc dùng chung component để không phá link từ Payment Result.

## 5. Kế hoạch triển khai

### FE-1. Chuẩn hóa type và API adapter

- Tạo type TypeScript khớp chính xác DTO của Order Service.
- Tạo API cho Checkout Session, Customer Order và Admin Order.
- Không gọi `fetch` trực tiếp; dùng `apiClient`.
- Chuẩn hóa query string bằng `URLSearchParams`.
- Tạo helper giữ `Idempotency-Key` ổn định cho một lần submit/command.

Kết quả mong đợi:

- Component không tự định nghĩa DTO rời rạc.
- Có một public API rõ ràng qua `features/checkout/index.ts` và `features/order/index.ts`.

### FE-2. Sửa Buy Now và khởi tạo Checkout Session

- Thay endpoint Buy Now không tồn tại bằng `POST /api/v1/checkout/sessions` với source `BUY_NOW`.
- Chuyển đến `/checkout?sessionId=...` sau khi tạo session.
- Luồng Cart tạo session source `CART` bằng cart ID authoritative.
- Nếu chưa đăng nhập, giữ `returnTo` nhưng không lưu giá/tồn ở browser.

Kết quả mong đợi:

- Cart và Buy Now đều đi vào cùng một Checkout Session contract.
- Refresh trang Checkout tải lại đúng session thay vì mất lựa chọn.

### FE-3. Tách Checkout khỏi Payment feature

- Chuyển wizard và API Checkout về `features/checkout`.
- Chỉ giữ Payment return/admin trong `features/payment`.
- Checkout cập nhật địa chỉ, payment timing/method qua Session API.
- Preview dùng endpoint của Order Service và render hoàn toàn từ response server.
- Xử lý các trạng thái `ACTIVE`, `COMPLETED`, `CANCELLED`, `EXPIRED`.
- Vô hiệu hóa submit khi đang gửi; retry cùng request dùng lại idempotency key.

Kết quả mong đợi:

- Không còn tạo Order bằng compatibility API trong luồng chính.
- Nhấn hai lần hoặc retry mạng không sinh hai Order.

### FE-4. Danh sách Order của Customer

- Tạo `/customer/account/orders`.
- Lọc theo trạng thái, mã đơn và thời gian.
- Có loading, empty, error và pagination.
- Link sang chi tiết bằng `orderId`.
- Sửa menu tài khoản để route tồn tại thực sự.

### FE-5. Chi tiết và timeline Customer Order

- Hiển thị item snapshot, địa chỉ nhận, breakdown tiền, voucher, shipping, payment và mốc thời gian.
- Timeline dùng dữ liệu đã được backend che actor/correlation nội bộ.
- Chỉ hiện nút `Đã nhận hàng` khi `availableActions` có `CONFIRM_RECEIVED`.
- Retry command giữ cùng idempotency key; sau thành công tải lại detail/timeline.
- Chuẩn hóa route `/customer/account/orders/{orderId}` và giữ compatibility cho link cũ.

### FE-6. Admin Order Workspace

- Thay `AdminSectionPage section="orders"` bằng màn dữ liệu thật.
- Hỗ trợ filter, pagination, chi tiết và timeline audit.
- Chỉ render thao tác phù hợp với `availableActions`.
- Thực hiện `pack`, `ship`, `handover` với confirmation và idempotency.
- Xử lý `401`, `403`, `404`, `409` bằng thông báo có ý nghĩa.

### FE-7. Guard và tích hợp Payment

- Customer chưa đăng nhập được chuyển tới Login với `returnTo` an toàn.
- Admin route kiểm tra UX role `ADMIN`; backend vẫn là nơi quyết định quyền cuối cùng.
- Payment Result điều hướng tới route Order chuẩn.
- Không sửa nghiệp vụ Payment ngoài điểm link/contract cần thiết; Hưng review thay đổi tích hợp.

### FE-8. Kiểm thử và nghiệm thu

Kiểm thử component/API tối thiểu:

- Tạo Checkout Session từ Cart và Buy Now.
- Refresh/khôi phục session.
- Session hết hạn.
- Preview đổi khi địa chỉ/voucher/payment thay đổi.
- Nhấn tạo Order hai lần không tạo trùng.
- Customer list/detail/timeline.
- Customer A không xem Order của Customer B.
- Admin filter/detail/pack/ship/handover.
- `401`, `403`, `404`, `409`, lỗi mạng và retry.

E2E qua Gateway:

1. Login Customer.
2. Chọn sản phẩm hoặc Buy Now.
3. Tạo Checkout Session.
4. Chọn địa chỉ, preview, chọn payment.
5. Tạo Order.
6. Kiểm tra Payment redirect/result khi cần.
7. Mở danh sách và chi tiết Order.
8. Admin xử lý Order.
9. Customer xác nhận đã nhận hàng.
10. Kiểm tra trạng thái cuối, timeline và không có bản ghi trùng.

## 6. Thứ tự ưu tiên

### P0 - Bắt buộc để luồng chính chạy đúng

1. FE-1: type/API adapter.
2. FE-2: Buy Now và Checkout Session.
3. FE-3: Checkout canonical.
4. FE-4: danh sách Customer Order.
5. FE-5: detail/timeline/received.

### P1 - Bắt buộc cho quản trị

6. FE-6: Admin Order Workspace.
7. FE-7: guard và liên kết Payment.

### P2 - Hoàn thiện chất lượng

8. FE-8: component test và E2E.
9. Responsive, accessibility và chuẩn hóa thông báo lỗi.

## 7. Definition of Done

Phần Frontend của Hiếu chỉ được coi là hoàn thành khi:

- Không còn gọi endpoint Checkout/Buy Now không tồn tại.
- Cart và Buy Now đều tạo/tải lại được Checkout Session thật.
- Create Order dùng endpoint canonical và `Idempotency-Key` ổn định.
- Customer có danh sách, chi tiết, timeline và xác nhận nhận hàng.
- Admin có danh sách, chi tiết, timeline và các command hợp lệ.
- Không có route trong menu Order dẫn tới 404.
- Không tin giá, discount, fee, stock, role hoặc trạng thái do client tự tính.
- `npm run lint` và `npm run build` đạt.
- Test luồng chính đạt qua Gateway với service thật.
- Thay đổi ở feature của thành viên khác được owner tương ứng review.

## 8. Tiến độ hiện tại

| Hạng mục | Trạng thái |
|---|---|
| Đồng bộ `origin/main` | Hoàn thành tại `b4cd3ff` |
| Xác nhận ownership Checkout/Order | Hoàn thành |
| Rà contract backend | Hoàn thành |
| Lint/build baseline | Hoàn thành |
| FE-1: Type và API adapter | Hoàn thành |
| FE-2: Buy Now và Checkout Session | Hoàn thành |
| FE-3: Checkout canonical | Hoàn thành |
| FE-4: Danh sách Customer Order | Hoàn thành |
| FE-5: Detail, timeline, xác nhận nhận hàng | Hoàn thành |
| FE-6: Admin Order Workspace | Hoàn thành |
| FE-7: Guard và tích hợp Payment | Hoàn thành trong phạm vi Checkout/Order; guard toàn khu Admin thuộc phần dùng chung |
| FE-8: Kiểm thử và nghiệm thu | Lint/build FE và 209 test Order Service đạt; chưa chạy E2E đủ toàn hệ thống |

Bước tiếp theo: chạy E2E qua Gateway khi Auth, Customer, Cart, Catalog, Promotion, Payment và Shipping cùng sẵn sàng; sau đó owner các feature liên quan review phần tích hợp chéo.
