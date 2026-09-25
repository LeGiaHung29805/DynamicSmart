import { apiClient } from "@/lib/api/client";

export type PaymentReturnStatus = {
  id: string;
  orderId: string;
  amountVnd: number;
  timing: "PREPAID" | "POSTPAID";
  method: "VNPAY" | "COD";
  status: "PENDING" | "PAID" | "FAILED" | "EXPIRED";
  paidAt?: string;
};

export const paymentReturnApi = {
  status: (reference: string) => apiClient.get<PaymentReturnStatus>(`api/v1/payments/vnpay/return-status?vnp_TxnRef=${encodeURIComponent(reference)}`),
};
