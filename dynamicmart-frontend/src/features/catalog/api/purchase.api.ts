import { apiClient } from "@/lib/api/client";
import { isCatalogStandaloneDemo } from "@/lib/api/config";

export function addVariantToCart(productId: string, variantId: string, quantity: number) {
  const path = isCatalogStandaloneDemo()
    ? "/api/v1/catalog/demo/cart/items"
    : "/api/v1/cart/items";
  return apiClient.post<void>(path, { productId, variantId, quantity });
}

/** Contract thuộc Checkout/Order; client chỉ gửi variantId và quantity, tuyệt đối không gửi giá/tồn. */
export function createBuyNowSession(variantId: string, quantity: number) {
  const path = isCatalogStandaloneDemo()
    ? "/api/v1/catalog/demo/checkout/sessions"
    : "/api/v1/checkout/sessions";
  return apiClient.post<{ id: string }>(path, {
    source: "BUY_NOW",
    variantId,
    quantity,
  });
}
