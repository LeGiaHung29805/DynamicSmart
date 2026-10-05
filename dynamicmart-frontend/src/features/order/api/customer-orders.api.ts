import { apiClient } from "@/lib/api/client";

export type CustomerOrder = {
  id: string;
  orderNumber: string;
  status: "PENDING_PAYMENT" | "CONFIRMED" | "PACKING" | "SHIPPING" | "HANDOVER_PENDING" | "DELIVERED" | "COMPLETED" | "CANCELLED";
  paymentTiming: "PREPAID" | "POSTPAID" | "NOT_REQUIRED";
  paymentMethod: string;
  finalTotalVnd: number;
  currency: string;
  canConfirmReceived: boolean;
  shipmentDeliveredAt?: string;
  completedAt?: string;
  createdAt: string;
};

export const customerOrdersApi = {
  get: (orderId: string) => apiClient.get<CustomerOrder>(`api/v1/orders/${orderId}`),
  confirmReceived: (orderId: string) => apiClient.post<CustomerOrder>(`api/v1/orders/${orderId}/confirm-received`),
};
