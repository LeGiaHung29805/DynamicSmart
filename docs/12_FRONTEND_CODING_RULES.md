# LUẬT FRONTEND — DYNAMICMART

> Frontend dùng Next.js App Router, React, TypeScript và Tailwind CSS. Mục tiêu là mỗi người triển khai domain của mình độc lập, nhưng vẫn dùng chung layout, giao diện và cách gọi API Gateway.

## 1. Nguyên tắc nền

- `dynamicmart-frontend` phải là ứng dụng Next.js; không tiếp tục mở rộng bộ khung Vite hiện có.
- Dùng App Router và thư mục `src/`. Trang/product public ưu tiên Server Component; chỉ thêm `'use client'` tại component nhỏ cần click, form, state hoặc browser API.
- Frontend chỉ gọi API Gateway qua `NEXT_PUBLIC_API_BASE_URL`, không gọi thẳng service/port hoặc database.
- Frontend hiển thị dữ liệu; tiền, tồn, eligibility voucher, phí GHN, quyền, trạng thái Order và payment phải do backend quyết định.
- Không lưu secret, JWT signing key, credential VNPay/GHN hoặc database credential ở frontend. Không lưu access token nhạy cảm trong `localStorage`.

## 2. Cấu trúc thư mục chuẩn

```text
src/
├── app/                              # Route, layout, metadata; không chứa business logic lớn
│   ├── (public)/                     # Login/Register/Quên mật khẩu
│   ├── page.tsx                       # Trang chủ `/` mặc định
│   ├── products/                      # Danh sách/chi tiết sản phẩm
│   ├── cart/                          # Giỏ hàng
│   ├── checkout/                      # Thanh toán/đặt hàng
│   ├── customer/account/             # Hồ sơ, địa chỉ, đơn, voucher
│   ├── admin/                        # Khu vực quản trị
│   ├── layout.tsx
│   ├── providers.tsx
│   ├── loading.tsx
│   ├── error.tsx
│   └── not-found.tsx
├── features/                         # Module theo domain; chủ sở hữu rõ ràng
│   ├── auth/
│   ├── catalog/
│   ├── customer/
│   ├── cart/
│   ├── promotion/
│   ├── checkout/
│   ├── order/
│   ├── payment/
│   ├── shipping/
│   ├── engagement/
│   └── reporting/
├── components/
│   ├── ui/                           # Button, Input, Modal, Table... không chứa nghiệp vụ
│   ├── common/                       # Price, EmptyState, Loading, StatusBadge...
│   └── layouts/                      # Header, Footer, Customer/Admin layout
├── lib/
│   ├── api/                          # Hạ tầng fetch, auth/session, error, config
│   ├── validation/
│   ├── constants/
│   └── utils/
├── contracts/
│   ├── api/                          # Kiểu request/response dùng chung đã chốt
│   └── generated/                    # Sinh từ OpenAPI; cấm sửa tay
├── hooks/                            # Hook thực sự dùng toàn app
├── mocks/                            # Fixture/MSW khi backend chưa sẵn sàng
├── styles/
└── types/                            # Kiểu toàn cục thật sự, không phải type domain
```

Không tạo `services/`, `helpers/`, `utils/` hoặc `components/` chung chung ở root để nhét mọi thứ vào. Một hàm/component thuộc nghiệp vụ nào thì ở `features/<nghiep-vu>/`.

## 3. Cấu trúc bắt buộc cho một Feature

Ví dụ `features/cart/`:

```text
features/cart/
├── api/                              # get-cart, add-cart-item, update-cart-item...
├── components/                       # CartItem, CartList, CartSummary...
├── hooks/                            # useCart, useAddCartItem...
├── schemas/                          # Zod validation cho form/input
├── types/                            # Kiểu UI riêng: CartViewModel...
├── utils/                            # Hàm thuần của Cart, không gọi API
├── CartPage.tsx                      # Composition cấp feature khi cần
└── index.ts                          # Public exports duy nhất của feature
```

- `app/.../page.tsx` chỉ ghép route với feature, ví dụ `return <CartPage />`; không gọi API trực tiếp hoặc chứa logic giỏ hàng.
- `features/<domain>/api/` chứa API của đúng domain đó; có cả phần frontend và API adapter của nghiệp vụ. Đây **không phải** backend API.
- `components/ui/` chỉ chứa component không biết Product, Cart, Order hay Voucher. `ProductCard`, `CartItem`, `OrderTimeline` phải ở feature sở hữu nghiệp vụ.
- Feature khác chỉ import từ `@/features/<name>` (qua `index.ts`), không import sâu vào `components/`, `api/` hoặc file nội bộ của feature đó.
- Tránh circular import giữa các feature. Nếu Checkout cần dữ liệu Cart thì nhận props/model từ public API của Cart hoặc dùng contract chung, không sửa file nội bộ Cart.

## 4. Quy tắc API bắt buộc

```text
Feature component/hook
        ↓
features/<domain>/api/<action>.ts
        ↓
lib/api/request.ts
        ↓
API Gateway
        ↓
Business service
```

`lib/api/` có cấu trúc tối thiểu:

```text
lib/api/
├── config.ts                         # NEXT_PUBLIC_API_BASE_URL
├── request.ts                        # Hàm fetch chuẩn duy nhất
├── client.ts                         # Adapter dùng từ Client Component
├── server.ts                         # Adapter dùng từ Server Component nếu cần
├── response.ts                       # ApiResponse, PageResponse
├── error.ts                          # ApiError và map lỗi thống nhất
└── session.ts                        # Quy tắc cookie/phiên đăng nhập
```

- Chỉ `lib/api/request.ts` được gọi `fetch` trực tiếp. Cấm gọi `fetch`, Axios hoặc URL Gateway rải rác trong component/hook.
- Một API function biểu đạt một action, ví dụ `features/order/api/create-order.ts`; không có file `api.service.ts` khổng lồ.
- Request/response public lấy từ `contracts/api/` hoặc `contracts/generated/`. UI state riêng để tại `features/<domain>/types/`; không biến DTO API thành state UI rồi sửa trực tiếp.
- Khi backend công bố OpenAPI, sinh type/client vào `contracts/generated/`; không sửa file sinh tự động.
- `credentials: 'include'` chỉ dùng theo hợp đồng session/cookie do Gateway và Identity chốt. UI không tự đọc/ghi cookie HttpOnly.
- Chuẩn hóa `401`, `403`, `404`, `409`, validation error và lỗi mạng ở `lib/api/error.ts`. Không tự hiển thị lỗi raw của backend.
- Không tự retry `POST` tạo Order, tạo payment hoặc callback. Request tạo Order/payment gửi `Idempotency-Key` theo API contract.
- Không tin hoặc tự tính `finalTotal`, `shippingFee`, voucher discount, stock hay role ở client; chỉ hiển thị response server.

## 5. State, form và dữ liệu server

- Dùng TanStack Query cho dữ liệu từ API: query key đặt tập trung trong feature, mutation invalidates query liên quan.
- Dùng React Hook Form + Zod cho form. Schema validation client chỉ giúp trải nghiệm; backend vẫn là nơi quyết định cuối cùng.
- Zustand chỉ dùng cho UI state nhỏ, dùng nhiều nơi và không nên đặt trong URL/server: ví dụ trạng thái mở menu mobile. Không dùng thay server cache hoặc làm nơi lưu token/payment/order thật.
- Filter/sort/pagination của Product List đặt trên URL search params để reload/chia sẻ link vẫn giữ trạng thái.
- Dữ liệu form Address chỉ cho nhập `addressLine`, người nhận, số điện thoại; Tỉnh/Thành phố và Phường/Xã chọn từ API danh mục GHN. Không cho text tự do ở hai trường địa giới.
- Checkout đổi địa chỉ, item, quantity, voucher hoặc shipping service phải bỏ preview/quote cũ và lấy lại từ server trước Create Order.

## 6. Authentication, route và phân quyền UI

- Route public: đăng ký, đăng nhập, quên/đặt lại mật khẩu, catalog. Route customer: profile, địa chỉ, cart, checkout, voucher, đơn hàng. Route admin: quản trị user/catalog/promotion/order/report.
- Route guard chỉ hỗ trợ trải nghiệm; backend luôn kiểm tra role/ownership lần nữa.
- Khi customer chưa đăng nhập bấm Mua ngay, UI giữ `returnUrl`; backend giữ Buy Now selection bằng cookie ký/TTL 15 phút và xác thực lại sau login. Frontend không tự giữ price/stock trong local storage.
- Không render dữ liệu quản trị vào bundle/trang khách chỉ rồi ẩn bằng CSS.

## 7. Component, styling và trải nghiệm chung

- `components/ui/` tối thiểu: Button, Input, Textarea, Select/Combobox, Checkbox, Radio, FormField, Modal/ConfirmDialog, Drawer, Table, Pagination, Toast, Skeleton.
- `components/common/` tối thiểu: Price, QuantityStepper, Image, StatusBadge, EmptyState, ErrorState, LoadingState.
- Mọi màn danh sách cần loading, empty, error và pagination. Action phá hủy như xóa địa chỉ/voucher cần ConfirmDialog.
- Component phải hỗ trợ mobile trước; modal bảng lớn dùng drawer/sheet phù hợp trên màn nhỏ.
- Không hard-code màu trạng thái hoặc text status ở nhiều nơi; dùng constants và StatusBadge chung.

## 8. Quyền sở hữu để tránh viết đè

| Khu vực | Người phụ trách/chủ sở hữu |
|---|---|
| `app/layout.tsx`, `providers.tsx`, `lib/api/`, `components/ui/`, `components/layouts/`, `contracts/` | Trưởng nhóm hoặc người được giao nền frontend; mọi thay đổi cần review chung |
| `features/auth/`, `features/catalog/` | Tiến |
| `features/customer/`, `features/cart/`, `features/promotion/` | Thảo |
| `features/checkout/`, `features/order/` | Hiếu |
| `features/payment/`, `features/shipping/` | Hưng |
| `features/engagement/`, `features/reporting/` | Tùng |

- Mỗi pull request chỉ sửa feature của mình và phần contract chung thật sự cần thiết.
- Không đổi tên/move hàng loạt file chung trong lúc các feature khác đang làm.
- Mọi thay đổi API/event/DTO public phải thông báo owner liên quan, cập nhật contract và fixture/mock trước khi nối API thật.
- Tên branch: `feature/<domain>-<task>`; không commit `.env`, file build hoặc token test.

## 9. Mock, test và Definition of Done

- Làm UI theo fixture/mock contract trước, sau đó thay mock bằng API thật mà không đổi component public.
- Mock phải mô phỏng các trạng thái: thành công, loading, empty, validation error, `401`, `403`, `409`, lỗi mạng và dữ liệu hết hạn khi cần.
- Test component cho luồng quan trọng: login, chọn variant, add cart, apply voucher, checkout, create order, payment result, order ownership/admin guard.
- E2E P0 chạy qua Gateway, không gọi service trực tiếp từ browser.
- Một feature chỉ hoàn thành khi: route hoạt động, responsive, có loading/empty/error, validation, API/mock contract, kiểm soát quyền hợp lý và test cho luồng chính.
