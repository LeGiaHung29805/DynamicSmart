import { apiClient } from "@/lib/api/client";
import type { OrderDetail, OrderItem, OrderPage, OrderQuery } from "../types/order.types";

export type CustomerOrder = OrderDetail;
export type CustomerOrderItem = OrderItem;

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

export type PurchasedOrderItem = {
  orderId: string;
  orderNumber: string;
  orderItemId: string;
  productId: string;
  variantId: string;
  productName: string;
  variantName?: string;
  imageUrl?: string;
  unitPriceVnd: number;
  quantity: number;
  completedAt?: string;
};

export const customerOrdersApi = {
  list: (query: OrderQuery = {}) => apiClient.get<OrderPage>(`api/v1/orders?${queryString(query)}`),
  get: (orderId: string) => apiClient.get<CustomerOrder>(`api/v1/orders/${orderId}`),
  getPurchasedItems: (productId?: string) =>
    apiClient.get<PurchasedOrderItem[]>(
      `api/v1/orders/purchased-items${productId ? `?productId=${productId}` : ""}`
    ),
  confirmReceived: async (orderId: string) => {
    await apiClient.post(`api/v1/orders/${orderId}/received`, undefined, {
      headers: { "Idempotency-Key": crypto.randomUUID() },
    });
    return apiClient.get<CustomerOrder>(`api/v1/orders/${orderId}`);
  },
};
