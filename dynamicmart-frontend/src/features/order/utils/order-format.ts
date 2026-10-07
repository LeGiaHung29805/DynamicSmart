import type { OrderStatus, PaymentMethod, PaymentTiming } from "../types/order.types";

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

const paymentMethods: Record<PaymentMethod, string> = {
  VNPAY: "VNPay",
  ZALOPAY: "ZaloPay",
  PAYOS: "PayOS",
  BANK_QR: "Chuyển khoản QR",
  COD: "Thanh toán khi nhận hàng (COD)",
  FREE: "Không cần thanh toán",
};

const paymentTimings: Record<PaymentTiming, string> = {
  PREPAID: "Thanh toán trước",
  POSTPAID: "Thanh toán khi bàn giao",
  NOT_REQUIRED: "Không cần thanh toán",
};

export function paymentMethodLabel(method: PaymentMethod | string) {
  return paymentMethods[method as PaymentMethod] ?? "Phương thức khác";
}

export function paymentTimingLabel(timing: PaymentTiming | string) {
  return paymentTimings[timing as PaymentTiming] ?? "Chưa xác định";
}

export function orderActorLabel(actor: string) {
  const actors: Record<string, string> = {
    CUSTOMER: "Khách hàng",
    ADMIN: "Quản trị viên",
    SYSTEM: "Hệ thống",
    PAYMENT: "Dịch vụ thanh toán",
  };
  return actors[actor] ?? "Hệ thống";
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
