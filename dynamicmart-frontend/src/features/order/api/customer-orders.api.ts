import { apiClient } from "@/lib/api/client";
import type { OrderPage, OrderQuery } from "../types/order.types";

export type CustomerOrderItem = {
  itemId: string;
  productId: string;
  variantId: string;
  productName: string;
  variantName?: string;
  imageUrl?: string;
  quantity: number;
  unitPriceVnd: number;
  lineTotalVnd: number;
};

export type CustomerOrder = {
  orderId: string;
  orderNumber: string;
  status: "PENDING_PAYMENT" | "CONFIRMED" | "PACKING" | "SHIPPING" | "HANDOVER_PENDING" | "DELIVERED" | "COMPLETED" | "CANCELLED";
  paymentTiming: "PREPAID" | "POSTPAID" | "NOT_REQUIRED";
  paymentMethod: string;
  money: { finalTotalVnd: number; currency: string };
  availableActions: string[];
  shipmentDeliveredAt?: string;
  completedAt?: string;
  createdAt: string;
  items?: CustomerOrderItem[];
};

function queryString(query: OrderQuery = {}) {
  const params = new URLSearchParams();
  params.set("page", String(query.page ?? 0));
  params.set("size", String(query.size ?? 20));
  if (query.status) params.set("status", query.status);
  if (query.orderNumber?.trim()) params.set("orderNumber", query.orderNumber.trim());
  if (query.createdFrom) params.set("createdFrom", query.createdFrom);
  if (query.createdTo) params.set("createdTo", query.createdTo);
  params.set("sort", query.sort ?? "createdAt,desc");
  return params.toString();
}

export const customerOrdersApi = {
  list: (query: OrderQuery = {}) => apiClient.get<OrderPage>(`api/v1/orders?${queryString(query)}`),
  get: (orderId: string) => apiClient.get<CustomerOrder>(`api/v1/orders/${orderId}`),
  confirmReceived: async (orderId: string) => {
    await apiClient.post(`api/v1/orders/${orderId}/received`, undefined, {
      headers: { "Idempotency-Key": crypto.randomUUID() },
    });
    return apiClient.get<CustomerOrder>(`api/v1/orders/${orderId}`);
  },
};
