import { categories } from "@/features/home";
import { demoBrands, demoProducts } from "./demoProducts";

export const categoryOptions = categories
  .map((item) => ({
    label: item.name,
    value: item.href.split("category=")[1],
    count: demoProducts.filter((product) => product.category === item.href.split("category=")[1]).length,
  }))
  .filter((item) => item.count > 0);

export const brandOptions = demoBrands.map((brand) => ({
  label: brand,
  value: brand.toLocaleLowerCase("vi-VN"),
  count: demoProducts.filter((product) => product.brand === brand).length,
}));

const priceBands = [
  { label: "Dưới 500.000₫", value: "under-500k", min: 0, max: 500_000 },
  { label: "500.000₫ – 1 triệu", value: "500k-1m", min: 500_000, max: 1_000_000 },
  { label: "1 – 2 triệu", value: "1m-2m", min: 1_000_000, max: 2_000_000 },
  { label: "Trên 2 triệu", value: "over-2m", min: 2_000_000, max: Number.POSITIVE_INFINITY },
] as const;

export const priceOptions = priceBands.map((item) => ({
  ...item,
  count: demoProducts.filter((product) => product.price >= item.min && product.price < item.max).length,
}));

export function matchesPrice(price: number, band: string): boolean {
  const option = priceOptions.find((item) => item.value === band);
  return !option || (price >= option.min && price < option.max);
}
