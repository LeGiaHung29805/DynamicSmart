import { apiClient } from "@/lib/api/client";

export function addVariantToCart(productId: string, variantId: string, quantity: number) {
  return apiClient.post<void>("/api/v1/cart/items", { productId, variantId, quantity });
}

export interface BuyNowSession {
  checkoutSessionId: string;
}

/** Contract thuộc Checkout/Order; client chỉ gửi variantId và quantity, tuyệt đối không gửi giá/tồn. */
export function createBuyNowSession(variantId: string, quantity: number) {
  return apiClient.post<BuyNowSession>("/api/v1/checkout-sessions/buy-now", { variantId, quantity });
}
