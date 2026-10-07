import { apiClient } from "@/lib/api/client";

export type PaymentStatus = "PENDING" | "PAID" | "FAILED" | "EXPIRED";

export type CustomerPayment = {
  id: string;
  orderId: string;
  amountVnd: number;
  timing: "PREPAID" | "POSTPAID";
  method: "VNPAY" | "ZALOPAY" | "PAYOS" | "BANK_QR" | "COD";
  status: PaymentStatus;
  redirectUrl?: string;
  expiresAt?: string;
  paidAt?: string;
  codConfirmedAt?: string;
};

export const customerPaymentsApi = {
  getByOrder: (orderId: string) =>
    apiClient.get<CustomerPayment>(`/api/v1/payments/my-orders/${orderId}`),
  createAttempt: (orderId: string) =>
    apiClient.post<CustomerPayment>(`/api/v1/payments/my-orders/${orderId}/attempts`),
};
