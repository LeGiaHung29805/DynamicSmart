import type { PaymentMethod } from "@/features/order/types/order.types";
import { apiClient } from "@/lib/api/client";

export type AvailablePaymentMethod = Exclude<PaymentMethod, "FREE">;

export type PaymentMethodsResponse = {
  methods: AvailablePaymentMethod[];
};

export const paymentMethodsApi = {
  available: () => apiClient.get<PaymentMethodsResponse>("/api/v1/payments/methods"),
};
