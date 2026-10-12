import type { OrderStatus, PaymentMethod, PaymentTiming } from "../types/order.types";

const labels: Record<string, string> = {
  PENDING_PAYMENT: "Chờ thanh toán",
  CONFIRMED: "Đã xác nhận",
  PACKING: "Đang đóng gói",
  SHIPPING: "Đang vận chuyển",
  HANDOVER_PENDING: "Chờ bàn giao",
  DELIVERED: "Đã giao hàng",
  COMPLETED: "Hoàn tất",
  CANCELLED: "Đã hủy",
  ORDER_CREATED: "Đã tạo đơn hàng",
  CREATED: "Mới tạo",
  PAID: "Đã thanh toán",
  FAILED: "Thanh toán thất bại",
  EXPIRED: "Đã hết hạn",
  PROCESSING: "Đang xử lý",
};

export function orderStatusLabel(status: OrderStatus | string) {
  return labels[status] ?? status;
}

const timelineReasonLabels: Record<string, string> = {
  ORDER_CREATED: "Khởi tạo đơn hàng thành công",
  ADMIN_PACK: "Quản trị viên xác nhận bắt đầu đóng gói kiện hàng",
  ADMIN_SHIP: "Quản trị viên chuyển kiện hàng cho đơn vị vận chuyển",
  ADMIN_HANDOVER: "Quản trị viên chuyển đơn sang trạng thái chờ bàn giao",
  CUSTOMER_CONFIRM_RECEIVED: "Khách hàng xác nhận đã nhận hàng thành công",
  PAYMENT_SUCCEEDED: "Ghi nhận thanh toán thành công",
  PAYMENT_FAILED: "Thanh toán thất bại",
  PAYMENT_EXPIRED: "Hết hạn thời gian thanh toán",
  PAYMENT_DUE: "Đơn hàng đến hạn thanh toán",
  CUSTOMER_CANCELLED: "Khách hàng yêu cầu hủy đơn",
  OUT_OF_STOCK: "Sản phẩm trong đơn đã hết hàng",
  EXPIRED: "Đơn hàng đã hết hạn xử lý",
  VOUCHER_RELEASED: "Đã hoàn lại mã giảm giá",
  INVENTORY_RELEASED: "Đã hoàn lại tồn kho",
};

export function orderTimelineReasonLabel(reason?: string | null): string {
  if (!reason) return "";
  return timelineReasonLabels[reason] ?? reason;
}

const cancelReasonLabels: Record<string, string> = {
  PAYMENT_FAILED: "Thanh toán không thành công",
  PAYMENT_EXPIRED: "Quá thời hạn thanh toán",
  CUSTOMER_CANCELLED: "Khách hàng yêu cầu hủy đơn",
  OUT_OF_STOCK: "Sản phẩm đã hết hàng",
  EXPIRED: "Đơn hàng đã hết hạn",
};

export function orderCancelReasonLabel(reason?: string | null): string {
  if (!reason) return "Không có lý do";
  return cancelReasonLabels[reason] ?? reason;
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

export function orderStatusTone(status: OrderStatus | string): "neutral" | "success" | "warning" | "danger" {
  if (status === "COMPLETED" || status === "DELIVERED" || status === "PAID") return "success";
  if (status === "CANCELLED" || status === "FAILED" || status === "EXPIRED") return "danger";
  if (status === "PENDING_PAYMENT" || status === "HANDOVER_PENDING" || status === "ORDER_CREATED" || status === "CREATED") return "warning";
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
