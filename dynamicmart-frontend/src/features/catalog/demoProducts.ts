import { bestSellers, newArrivals } from "@/features/home";

// Bộ dữ liệu minh họa giao diện; thay bằng Catalog API khi contract/backend sẵn sàng.
export const demoProducts = [
  { ...bestSellers[0], category: "thoi-trang-nu", isNew: false },
  { ...bestSellers[1], category: "dong-ho", isNew: false },
  { ...bestSellers[2], category: "giay-dep", isNew: false },
  { ...bestSellers[3], category: "tui-phu-kien", isNew: false },
  { ...newArrivals[0], category: "thoi-trang-nam", isNew: true },
  { ...newArrivals[1], category: "tui-phu-kien", isNew: true },
  { ...newArrivals[2], category: "giay-dep", isNew: true },
  { ...newArrivals[3], category: "tui-phu-kien", isNew: true },
] as const;

export type DemoProduct = (typeof demoProducts)[number];

export const demoBrands = Array.from(new Set(demoProducts.map((product) => product.brand))).sort((a, b) => a.localeCompare(b));
