import type { OrderStatus } from "../types/order.types";

const labels: Record<OrderStatus, string> = {
  PENDING_PAYMENT: "Chờ thanh toán",
  CONFIRMED: "Đã xác nhận",
  PACKING: "Đang đóng gói",
  SHIPPING: "Đang vận chuyển",
  HANDOVER_PENDING: "Chờ bàn giao",
  DELIVERED: "Đã giao",
  COMPLETED: "Hoàn tất",
  CANCELLED: "Đã hủy",
};

export function orderStatusLabel(status: OrderStatus) {
  return labels[status];
}

export function orderStatusTone(status: OrderStatus): "neutral" | "success" | "warning" | "danger" {
  if (status === "COMPLETED" || status === "DELIVERED") return "success";
  if (status === "CANCELLED") return "danger";
  if (status === "PENDING_PAYMENT" || status === "HANDOVER_PENDING") return "warning";
  return "neutral";
}

export function formatVnd(value: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value);
}

export function formatDateTime(value?: string) {
  return value ? new Date(value).toLocaleString("vi-VN") : "—";
}
