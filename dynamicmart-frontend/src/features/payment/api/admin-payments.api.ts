import { apiClient } from "@/lib/api/client";

export type AdminPayment = {
  id: string;
  orderId: string;
  amountVnd: number;
  timing: "PREPAID" | "POSTPAID";
  method: "VNPAY" | "COD";
  status: "PENDING" | "PAID" | "FAILED" | "EXPIRED";
  expiresAt?: string;
  paidAt?: string;
  codReceiptNo?: string;
  codConfirmedBy?: string;
  codConfirmedAt?: string;
};

export type PaymentPage = { content: AdminPayment[]; page: number; size: number; totalElements: number; totalPages: number };
export type PaymentAttempt = { id: string; attemptNo: number; provider: string; reference?: string; amountVnd: number; status: string; expiresAt?: string; createdAt: string };
export type CallbackAudit = { id: string; providerReference?: string; checksumValid: boolean; amountValid: boolean; processedResult: string; receivedAt: string };

export const adminPaymentsApi = {
  list: (page = 0) => apiClient.get<PaymentPage>(`api/v1/payments?page=${page}&size=20`),
  attempts: (paymentId: string) => apiClient.get<PaymentAttempt[]>(`api/v1/payments/${paymentId}/attempts`),
  audits: (paymentId: string) => apiClient.get<CallbackAudit[]>(`api/v1/payments/${paymentId}/callback-audits`),
  confirmCod: (paymentId: string, collectedAmountVnd: number, receiptNo: string) => apiClient.post<AdminPayment>(`api/v1/payments/${paymentId}/cod-confirmations`, { collectedAmountVnd, receiptNo }),
};
