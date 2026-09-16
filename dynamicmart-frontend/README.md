# DynamicMart Frontend

Frontend của DynamicMart dùng Next.js App Router, React, TypeScript và Tailwind CSS.

## Chạy dự án

```powershell
Copy-Item .env.example .env.local
npm install
npm run dev
```

Mở `http://localhost:3000`. Chỉ đổi `NEXT_PUBLIC_API_BASE_URL` thành URL của API Gateway; không đặt URL từng business service hoặc bất kỳ secret nào tại frontend.

## Những phần dùng chung đã có

```text
src/app/(store)/                  Layout cửa hàng dùng chung cho trang chủ và các trang mua sắm
src/app/(auth)/                   Layout đăng nhập/đăng ký/quên mật khẩu dùng chung
src/app/layout.tsx               Root layout và providers cho toàn bộ ứng dụng
src/components/ui/               Button, Input, Select, Toast
src/components/common/           PageHeader, SurfacePanel, Price, StatusBadge, Loading/Empty/Error state
src/components/layouts/          Layout khách, tài khoản customer, quản trị
src/lib/api/                     Lớp fetch duy nhất qua Gateway
src/contracts/api/               Kiểu response/error dùng chung
src/features/README.md           Mẫu tổ chức và owner của từng domain
```

Mỗi người tạo code nghiệp vụ trong `src/features/<domain>/`. `app/.../page.tsx` chỉ ghép route và feature, không chứa business logic hoặc gọi `fetch` trực tiếp.

Đặt trang mua sắm mới trong `src/app/(store)/.../page.tsx` để tự nhận header/footer của cửa hàng. Đặt trang xác thực mới trong `src/app/(auth)/.../page.tsx` để tự nhận khung auth; dùng `AuthPageContent` cho tiêu đề, mô tả và phần nội dung trang. Tên nhóm trong ngoặc không xuất hiện trên URL: ví dụ `(auth)/login/page.tsx` vẫn là `/login`. Các trang admin/customer giữ layout riêng.

## Mẫu để đồng bộ giao diện giữa các nhóm

- `/products` và `/products/[id]` là mẫu trang công khai trong `(store)`, dùng `PageHeader`, `ProductCard` và `CatalogFilterControls`. Bộ lọc có sidebar desktop, ngăn kéo mobile, chip đang áp dụng, tìm kiếm, thương hiệu, danh mục, khoảng giá, giảm giá và sắp xếp; trạng thái nằm trong query URL. Danh sách hiện là dữ liệu minh họa từ `features/catalog/demoProducts.ts`, chưa phải Catalog API; nút thêm giỏ hàng chưa hoạt động.
- `/admin` là dashboard mẫu; `/admin/users`, `/admin/catalog`, `/admin/orders`, `/admin/reports` cùng dùng `AdminSectionPage`. Thay trạng thái/bảng trống bằng API thật ở feature của từng domain, không tự dựng sidebar, tiêu đề, bộ lọc và table style khác nhau.
- Trang mới chỉ lắp ghép component trong `app/.../page.tsx`. Dùng `PageHeader` cho tên trang, `SurfacePanel` cho khối nội dung, `Input`/`Select` cho form, `StatusBadge` cho trạng thái và `LoadingState`/`EmptyState`/`ErrorState` cho dữ liệu bất đồng bộ. Không đưa số liệu quản trị giả vào trang thật.

Các trang admin trên mới là khung UI, chưa nối API hoặc kiểm soát quyền truy cập. Không dùng làm trang quản trị production trước khi có bảo vệ vai trò `ADMIN` và xử lý dữ liệu thật.

## Quy tắc gọi API

```text
Feature component/hook
→ features/<domain>/api/<action>.ts
→ lib/api/client.ts hoặc lib/api/server.ts
→ lib/api/request.ts
→ API Gateway
```

Ví dụ API của giỏ hàng:

```ts
import { apiClient } from "@/lib/api/client";

export function getCart() {
  return apiClient.get<CartResponse>("/api/v1/cart");
}
```

Không gọi `fetch` trực tiếp trong component; không gọi URL/port của `cart-service`, `order-service` hay service khác.

Chi tiết đầy đủ nằm trong [luật frontend](../docs/12_FRONTEND_CODING_RULES.md).
