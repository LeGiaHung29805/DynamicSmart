import { apiClient } from "@/lib/api/client";
import type { CartDto, CartItemDto } from "../types/cart.types";

type CompatibleCartItem = Partial<CartItemDto> & {
  id: string;
  quantity?: number;
  version?: number;
  selected?: boolean;
  available?: boolean;
  image?: string;
  listPrice?: number;
  salePrice?: number;
};

type CompatibleCart = Omit<Partial<CartDto>, "items"> & {
  id?: string;
  customerId?: string;
  items?: CompatibleCartItem[];
  lines?: CompatibleCartItem[];
};

function money(value: unknown, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback;
}

/** Giữ giao diện Thảo tương thích cả response Cart cũ và response Cart mới. */
function normalizeCart(value: CompatibleCart): CartDto {
  const items = value.items ?? value.lines ?? [];
  return {
    id: value.id ?? "",
    customerId: value.customerId ?? "",
    updatedAt: value.updatedAt ?? new Date(0).toISOString(),
    items: items.map((item) => {
      const listPriceVnd = money(item.listPriceVnd ?? item.listPrice ?? item.salePriceVnd ?? item.salePrice);
      const salePriceVnd = money(item.salePriceVnd ?? item.salePrice, listPriceVnd);
      return {
        id: item.id,
        productId: item.productId ?? "",
        variantId: item.variantId ?? "",
        productName: item.productName,
        variantName: item.variantName,
        imageUrl: item.imageUrl ?? item.image,
        quantity: Math.max(1, Number(item.quantity) || 1),
        version: Number(item.version) || 0,
        selected: item.selected !== false,
        purchasable: item.purchasable ?? item.available ?? true,
        availableQuantity: Math.max(0, Number(item.availableQuantity) || 99),
        unavailableReason: item.unavailableReason,
        listPriceVnd,
        salePriceVnd,
        updatedAt: item.updatedAt ?? value.updatedAt ?? new Date(0).toISOString(),
      };
    }),
  };
}

export const cartApi = {
  get: () => apiClient.get<CompatibleCart>("/api/v1/cart").then(normalizeCart),
  add: (productId: string, variantId: string, quantity: number) => apiClient.post<CompatibleCart>("/api/v1/cart/items", { productId, variantId, quantity }).then(normalizeCart),
  update: (itemId: string, quantity: number, selected: boolean, version: number) => apiClient.patch<CompatibleCart>(`/api/v1/cart/items/${itemId}`, { quantity, selected, version }).then(normalizeCart),
  remove: (itemId: string) => apiClient.delete<void>(`/api/v1/cart/items/${itemId}`),
};
