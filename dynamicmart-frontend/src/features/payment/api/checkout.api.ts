import { apiClient } from "@/lib/api/client";

export type CheckoutPreviewItem = { cartItemId: string; cartItemVersion: number; productId: string; categoryId: string; variantId: string; sku: string; productName: string; variantName?: string; imageUrl?: string; directSalePromotionId?: string; quantity: number; listPriceVnd: number; directSaleDiscountVnd: number; unitPriceVnd: number; lineTotalVnd: number };
export type CheckoutVoucher = { id: string; code: string; name: string; scope: string; eligible: boolean; ineligibleReason?: string; discountAmountVnd: number };
export type CheckoutPreview = { cartId: string; items: CheckoutPreviewItem[]; listSubtotalVnd: number; directSaleDiscountVnd: number; itemsSubtotalVnd: number; merchandiseVoucher?: CheckoutVoucher; shippingVoucher?: CheckoutVoucher; merchandiseDiscountVnd: number; shippingDiscountVnd: number; shippingQuote: { quoteId: string; feeVnd: number; payableFeeVnd: number; serviceName: string; eta: string; expiresAt: string }; finalTotalVnd: number };
export type CheckoutSelection = { addressId: string; merchandiseVoucherId?: string; shippingVoucherId?: string };
export type OrderResult = { orderId: string; orderNumber: string; status: string; redirectUrl?: string };

export const checkoutApi = {
  preview: (selection: CheckoutSelection) => apiClient.post<CheckoutPreview>("/api/v1/cart/checkout/preview", selection),
  createOrder: (selection: CheckoutSelection & { cartId: string; quoteId: string; paymentTiming: string; paymentMethod: string }, idempotencyKey: string) => apiClient.post<OrderResult>("/api/v1/checkout/orders", selection, { headers: { "Idempotency-Key": idempotencyKey } }),
};
