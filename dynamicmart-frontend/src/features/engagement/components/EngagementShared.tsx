import { StatusBadge } from "@/components/common/StatusBadge";

export const money = (value: number) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value);
export const date = (value: string) => new Date(value).toLocaleDateString("vi-VN");

export function engagementStatusLabel(value: string) {
  return ({
    VISIBLE: "Đang hiển thị",
    HIDDEN: "Đã ẩn",
    PENDING: "Chờ xử lý",
    ANSWERED: "Đã trả lời",
    OPEN: "Đang mở",
    CLOSED: "Đã đóng",
    RESOLVED: "Đã giải quyết",
    REJECTED: "Đã từ chối",
  } as Record<string, string>)[value] ?? "Chưa xác định";
}

export function notificationTypeLabel(value: string) {
  if (value.includes("ORDER")) return "Cập nhật đơn hàng";
  if (value.includes("PAYMENT")) return "Cập nhật thanh toán";
  if (value.includes("PROMOTION") || value.includes("VOUCHER")) return "Khuyến mãi";
  if (value.includes("REVIEW")) return "Đánh giá sản phẩm";
  return "Thông báo hệ thống";
}

export function Status({ value }: Readonly<{ value: string }>) {
  const tone = value === "VISIBLE" || value === "ANSWERED" || value === "OPEN" ? "success" : value === "HIDDEN" ? "danger" : "warning";
  return <StatusBadge label={engagementStatusLabel(value)} tone={tone} />;
}
