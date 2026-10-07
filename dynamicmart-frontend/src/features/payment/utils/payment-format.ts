import type { PaymentStatus } from "../api/customer-payments.api";

const statusLabels: Record<PaymentStatus, string> = {
  PENDING: "Chờ thanh toán",
  PAID: "Đã thanh toán",
  FAILED: "Thanh toán thất bại",
  EXPIRED: "Đã hết hạn",
};

export function paymentStatusLabel(status: PaymentStatus | string) {
  return statusLabels[status as PaymentStatus] ?? "Chưa xác định";
}

export function paymentStatusTone(status: PaymentStatus | string): "neutral" | "success" | "warning" | "danger" {
  if (status === "PAID") return "success";
  if (status === "PENDING") return "warning";
  if (status === "FAILED" || status === "EXPIRED") return "danger";
  return "neutral";
}

export function paymentAttemptStatusLabel(status: string) {
  const labels: Record<string, string> = {
    CREATED: "Đã tạo đường dẫn",
    REDIRECTED: "Đã chuyển sang cổng thanh toán",
    SUCCEEDED: "Thành công",
    FAILED: "Thất bại",
    EXPIRED: "Hết hạn",
  };
  return labels[status] ?? "Chưa xác định";
}

export function callbackResultLabel(result: string) {
  const labels: Record<string, string> = {
    APPLIED: "Đã áp dụng",
    DUPLICATE: "Thông báo trùng",
    LATE_AUDIT: "Đến muộn, chỉ lưu kiểm tra",
    REJECTED: "Bị từ chối",
  };
  return labels[result] ?? "Chưa xác định";
}
