import { StatusBadge } from "@/components/common/StatusBadge";

export const money = (value: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value);

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

const htmlEntitiesMap: Record<string, string> = {
  "&amp;": "&",
  "&lt;": "<",
  "&gt;": ">",
  "&quot;": "\"",
  "&#39;": "'",
  "&agrave;": "à",
  "&aacute;": "á",
  "&acirc;": "â",
  "&atilde;": "ã",
  "&egrave;": "è",
  "&eacute;": "é",
  "&ecirc;": "ê",
  "&igrave;": "ì",
  "&iacute;": "í",
  "&ograve;": "ò",
  "&oacute;": "ó",
  "&ocirc;": "ô",
  "&otilde;": "õ",
  "&ugrave;": "ù",
  "&uacute;": "ú",
  "&yacute;": "ý",
  "&Agrave;": "À",
  "&Aacute;": "Á",
  "&Acirc;": "Â",
  "&Atilde;": "Ã",
  "&Egrave;": "È",
  "&Eacute;": "É",
  "&Ecirc;": "Ê",
  "&Igrave;": "Ì",
  "&Iacute;": "Í",
  "&Ograve;": "Ò",
  "&Oacute;": "Ó",
  "&Ocirc;": "Ô",
  "&Otilde;": "Õ",
  "&Ugrave;": "Ù",
  "&Uacute;": "Ú",
  "&Yacute;": "Ý",
};

/**
 * Decodes HTML entities (e.g. &agrave;, &ecirc;, &oacute;, &amp;) back to clean UTF-8 text.
 */
export function decodeHtml(text: string | null | undefined): string {
  if (!text) return "";
  if (!text.includes("&")) return text;
  if (typeof document !== "undefined") {
    try {
      const doc = new DOMParser().parseFromString(text, "text/html");
      const decoded = doc.documentElement.textContent;
      if (decoded) return decoded;
    } catch {
      // fallback
    }
  }
  return text.replace(/&[#\w]+;/g, (entity) => htmlEntitiesMap[entity] || entity);
}
