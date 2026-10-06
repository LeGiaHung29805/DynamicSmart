import { apiClient } from "@/lib/api/client";
import type { OrderCommandResult, OrderDetail, OrderPage, OrderQuery, OrderTimeline } from "../types/order.types";

function queryString(query: OrderQuery = {}) {
  const params = new URLSearchParams();
  params.set("page", String(query.page ?? 0));
  params.set("size", String(query.size ?? 20));
  if (query.status) params.set("status", query.status);
  if (query.orderNumber?.trim()) params.set("orderNumber", query.orderNumber.trim());
  if (query.customerId?.trim()) params.set("customerId", query.customerId.trim());
  if (query.createdFrom) params.set("createdFrom", query.createdFrom);
  if (query.createdTo) params.set("createdTo", query.createdTo);
  params.set("sort", query.sort ?? "createdAt,desc");
  return params.toString();
}

function command(orderId: string, action: "pack" | "ship" | "handover", idempotencyKey: string) {
  return apiClient.post<OrderCommandResult>(`/api/v1/orders/admin/${orderId}/${action}`, undefined, {
    headers: { "Idempotency-Key": idempotencyKey },
  });
}

export const adminOrdersApi = {
  list: (query: OrderQuery = {}) => apiClient.get<OrderPage>(`/api/v1/orders/admin?${queryString(query)}`),
  get: (orderId: string) => apiClient.get<OrderDetail>(`/api/v1/orders/admin/${orderId}`),
  timeline: (orderId: string) => apiClient.get<OrderTimeline>(`/api/v1/orders/admin/${orderId}/timeline`),
  pack: (orderId: string, key: string) => command(orderId, "pack", key),
  ship: (orderId: string, key: string) => command(orderId, "ship", key),
  handover: (orderId: string, key: string) => command(orderId, "handover", key),
};
