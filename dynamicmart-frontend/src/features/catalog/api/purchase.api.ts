import { apiClient } from "@/lib/api/client";

export function addVariantToCart(productId: string, variantId: string, quantity: number) {
  return apiClient.post<void>("/api/v1/cart/items", { productId, variantId, quantity });
}

export interface BuyNowSession {
  checkoutSessionId: string;
}

/** Contract thuộc Checkout/Order; client chỉ gửi variantId và quantity, tuyệt đối không gửi giá/tồn. */
export async function createBuyNowSession(variantId: string, quantity: number) {
  const session = await apiClient.post<{ id: string }>("/api/v1/checkout/sessions", {
    source: "BUY_NOW",
    variantId,
    quantity,
  });
  return { checkoutSessionId: session.id } satisfies BuyNowSession;
}
