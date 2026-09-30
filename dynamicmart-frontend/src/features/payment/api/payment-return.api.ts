import { apiClient } from "@/lib/api/client";

export type PaymentReturnStatus = {
  id: string;
  orderId: string;
  amountVnd: number;
  timing: "PREPAID" | "POSTPAID";
  method: "VNPAY" | "ZALOPAY" | "PAYOS" | "BANK_QR" | "COD";
  status: "PENDING" | "PAID" | "FAILED" | "EXPIRED";
  paidAt?: string;
};

export const paymentReturnApi = {
  status: (reference: string) => apiClient.get<PaymentReturnStatus>(`api/v1/payments/return-status?reference=${encodeURIComponent(reference)}`),
};
