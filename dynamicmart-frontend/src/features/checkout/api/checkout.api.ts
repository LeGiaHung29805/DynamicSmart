import { apiClient } from "@/lib/api/client";
import type {
  CheckoutPreview,
  CheckoutSession,
  CreateCheckoutSessionInput,
  CreateOrderResult,
  UpdateCheckoutSessionInput,
} from "../types/checkout.types";

export const checkoutApi = {
  createSession: (input: CreateCheckoutSessionInput) => apiClient.post<CheckoutSession>("/api/v1/checkout/sessions", input),
  getSession: (sessionId: string) => apiClient.get<CheckoutSession>(`/api/v1/checkout/sessions/${sessionId}`),
  updateSession: (sessionId: string, input: UpdateCheckoutSessionInput) => apiClient.patch<CheckoutSession>(`/api/v1/checkout/sessions/${sessionId}`, input),
  preview: (sessionId: string, input: { merchandiseVoucherId?: string; shippingVoucherId?: string; serviceCode?: string }) =>
    apiClient.post<CheckoutPreview>(`/api/v1/checkout/sessions/${sessionId}/preview`, input),
  cancel: (sessionId: string) => apiClient.delete<CheckoutSession>(`/api/v1/checkout/sessions/${sessionId}`),
  createOrder: (sessionId: string, idempotencyKey: string) => apiClient.post<CreateOrderResult>(
    `/api/v1/checkout/sessions/${sessionId}/orders`, undefined, { headers: { "Idempotency-Key": idempotencyKey } },
  ),
};
