import { apiClient } from "@/lib/api/client";
import type { CheckoutSession } from "@/features/checkout";

export function addVariantToCart(productId: string, variantId: string, quantity: number) {
  return apiClient.post<void>("/api/v1/cart/items", { productId, variantId, quantity });
}

/** Contract thuộc Checkout/Order; client chỉ gửi variantId và quantity, tuyệt đối không gửi giá/tồn. */
export function createBuyNowSession(variantId: string, quantity: number) {
  return apiClient.post<CheckoutSession>("/api/v1/checkout/sessions", {
    source: "BUY_NOW",
    variantId,
    quantity,
  });
}
