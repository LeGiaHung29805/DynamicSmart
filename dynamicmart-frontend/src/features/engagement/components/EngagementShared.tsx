import { StatusBadge } from "@/components/common/StatusBadge";

export const money = (value: number) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value);
export const date = (value: string) => new Date(value).toLocaleDateString("vi-VN");

export function Status({ value }: Readonly<{ value: string }>) {
  const tone = value === "VISIBLE" || value === "ANSWERED" || value === "OPEN" ? "success" : value === "HIDDEN" ? "danger" : "warning";
  return <StatusBadge label={value} tone={tone} />;
}
