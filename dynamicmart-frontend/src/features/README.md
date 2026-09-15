# QUY ƯỚC FEATURES

Mỗi người chỉ tạo/sửa module thuộc domain được giao. Một feature gồm `api/`, `components/`, `hooks/`, `schemas/`, `types/`, `utils/` và `index.ts` khi có phần công khai cho feature khác dùng.

```text
features/cart/
├── api/get-cart.ts
├── components/CartPage.tsx
├── hooks/use-cart.ts
├── schemas/cart.schema.ts
├── types/cart.types.ts
├── utils/cart.utils.ts
└── index.ts
```

`api/` là adapter gọi Gateway của domain đó; không gọi `fetch` trực tiếp mà dùng `@/lib/api/client` hoặc `@/lib/api/server`.

| Domain | Chủ sở hữu |
|---|---|
| `home` | Nền frontend chung; Tiến nối dữ liệu Catalog thật sau khi contract sẵn sàng |
| `auth`, `catalog` | Tiến |
| `customer`, `cart`, `promotion` | Thảo |
| `checkout`, `order` | Hiếu |
| `payment`, `shipping` | Hưng |
| `engagement`, `reporting` | Tùng |
