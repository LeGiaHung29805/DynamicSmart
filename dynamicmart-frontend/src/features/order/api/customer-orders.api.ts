import { apiClient } from "@/lib/api/client";
import type { OrderCommandResult, OrderDetail, OrderPage, OrderQuery, OrderTimeline } from "../types/order.types";

function queryString(query: OrderQuery = {}) {
  const params = new URLSearchParams();
  if (query.page !== undefined) params.set("page", String(query.page));
  if (query.size !== undefined) params.set("size", String(query.size));
  if (query.status) params.set("status", query.status);
  if (query.orderNumber?.trim()) params.set("orderNumber", query.orderNumber.trim());
  if (query.createdFrom) params.set("createdFrom", query.createdFrom);
  if (query.createdTo) params.set("createdTo", query.createdTo);
  if (query.sort) params.set("sort", query.sort);
  return params.toString();
}

export const customerOrdersApi = {
  list: (query: OrderQuery = {}) => apiClient.get<OrderPage>(`/api/v1/orders?${queryString(query)}`),
  get: (orderId: string) => apiClient.get<OrderDetail>(`/api/v1/orders/${orderId}`),
  timeline: (orderId: string) => apiClient.get<OrderTimeline>(`/api/v1/orders/${orderId}/timeline`),
  confirmReceived: (orderId: string, idempotencyKey: string) => apiClient.post<OrderCommandResult>(
    `/api/v1/orders/${orderId}/received`, undefined, { headers: { "Idempotency-Key": idempotencyKey } },
  ),
};
