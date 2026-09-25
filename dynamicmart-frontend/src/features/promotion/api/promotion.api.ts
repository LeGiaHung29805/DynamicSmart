import { apiClient } from "@/lib/api/client";

export type DirectSaleDto = { id: string; name: string; description?: string; status: string; discountMethod: string; fixedDiscountVnd?: number; discountRateBps?: number; maxDiscountVnd?: number; startsAt: string; endsAt: string; variantIds: string[]; updatedAt: string };
export type VoucherDto = { id: string; code: string; name: string; description?: string; status: string; scope: string; discountMethod: string; fixedDiscountVnd?: number; discountRateBps?: number; maxDiscountVnd?: number; minimumOrderVnd?: number; minimumEligibleSubtotalVnd?: number; usageLimit?: number; consumedCount: number; usageLimitPerCustomer?: number; startsAt: string; endsAt: string; distributionMode: string; defaultVoucher: boolean; productIds: string[]; categoryIds: string[] };
export type PromotionAuditDto = { id: string; actorAdminId: string; targetType: string; targetId: string; action: string; reason: string; createdAt: string };
export type VoucherReservationDto = { id: string; voucherId: string; customerId: string; checkoutSessionId: string; orderId?: string; status: string; discountAmountVnd: number; shippingDiscountVnd: number; reservedUntil: string };

const key = () => ({ headers: { "Idempotency-Key": crypto.randomUUID() } });
export const promotionApi = {
  directSales: () => apiClient.get<DirectSaleDto[]>("/api/v1/cart/admin/direct-sales"),
  vouchers: () => apiClient.get<VoucherDto[]>("/api/v1/cart/admin/vouchers"),
  createDirectSale: (body: object) => apiClient.post<DirectSaleDto>("/api/v1/cart/admin/direct-sales", body, key()),
  createVoucher: (body: object) => apiClient.post<VoucherDto>("/api/v1/cart/admin/vouchers", body, key()),
  updateDirectSale: (id: string, body: object) => apiClient.put<DirectSaleDto>(`/api/v1/cart/admin/direct-sales/${id}`, body, key()),
  updateVoucher: (id: string, body: object) => apiClient.put<VoucherDto>(`/api/v1/cart/admin/vouchers/${id}`, body, key()),
  setDirectSaleStatus: (id: string, status: string, reason: string) => apiClient.patch<DirectSaleDto>(`/api/v1/cart/admin/direct-sales/${id}/status`, { status, reason }, key()),
  setVoucherStatus: (id: string, status: string, reason: string) => apiClient.patch<VoucherDto>(`/api/v1/cart/admin/vouchers/${id}/status`, { status, reason }, key()),
  assignVoucher: (id: string, customerId: string, expiresAt: string | undefined, reason: string) => apiClient.post(`/api/v1/cart/admin/vouchers/${id}/assignments`, { customerId, expiresAt, reason }, key()),
  directSaleAudits: (id: string) => apiClient.get<PromotionAuditDto[]>(`/api/v1/cart/admin/direct-sales/${id}/audits`),
  voucherAudits: (id: string) => apiClient.get<PromotionAuditDto[]>(`/api/v1/cart/admin/vouchers/${id}/audits`),
  reservationHistory: () => apiClient.get<VoucherReservationDto[]>("/api/v1/cart/admin/voucher-reservations"),
};
