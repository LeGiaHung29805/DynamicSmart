import { apiClient } from "@/lib/api/client";
import type { CartDto } from "../types/cart.types";
export const cartApi = {
  get: () => apiClient.get<CartDto>("/api/v1/cart"),
  add: (productId: string, variantId: string, quantity: number) => apiClient.post<CartDto>("/api/v1/cart/items", { productId, variantId, quantity }),
  update: (itemId: string, quantity: number, selected: boolean, version: number) => apiClient.patch<CartDto>(`/api/v1/cart/items/${itemId}`, { quantity, selected, version }),
  remove: (itemId: string) => apiClient.delete<void>(`/api/v1/cart/items/${itemId}`),
};
