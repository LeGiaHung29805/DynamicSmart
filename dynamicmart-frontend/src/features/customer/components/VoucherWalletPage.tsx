"use client";

import { CalendarClock, Copy, TicketPercent } from "lucide-react";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/Button";
import { customerApi } from "../api/customer.api";
import type { VoucherWalletItem } from "../types/customer.types";

export function VoucherWalletPage() {
  const [items, setItems] = useState<VoucherWalletItem[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState(false);
  const [copied, setCopied] = useState("");
  useEffect(() => { customerApi.vouchers().then(setItems).catch(() => setError(true)).finally(() => setLoading(false)); }, []);
  const copy = async (code: string) => { await navigator.clipboard.writeText(code); setCopied(code); window.setTimeout(() => setCopied(""), 1800); };
  return <div className="space-y-7"><PageHeader eyebrow="Ưu đãi dành cho bạn" title="Kho voucher" description="Xem mã đủ điều kiện, sao chép mã và chọn lại tại Checkout. Số tiền giảm cuối cùng luôn do máy chủ tính." />
    <div className="overflow-hidden rounded-3xl bg-gradient-to-r from-rose-700 via-rose-600 to-orange-500 px-6 py-8 text-white shadow-lg sm:px-10"><p className="text-sm font-bold uppercase tracking-[0.2em] text-rose-100">DynamicMart Rewards</p><h2 className="mt-2 text-3xl font-black tracking-tight">Ưu đãi dành riêng cho bạn</h2><p className="mt-3 max-w-2xl text-sm leading-6 text-rose-50">Voucher mặc định và voucher được cấp riêng sẽ xuất hiện tại đây. Mã chưa đủ điều kiện vẫn hiển thị kèm lý do.</p></div>
    {loading ? <SurfacePanel>Đang tải voucher…</SurfacePanel> : error ? <SurfacePanel className="text-red-600">Không thể tải ví voucher.</SurfacePanel> : items.length === 0 ? <SurfacePanel>Ví voucher hiện đang trống.</SurfacePanel> :
      <div className="grid gap-5 lg:grid-cols-2">{items.map((voucher) => <article className={`relative overflow-hidden rounded-2xl border bg-white shadow-sm ${voucher.eligible ? "border-stone-200" : "border-amber-200"}`} key={voucher.id}><span className="absolute -left-3 top-1/2 size-6 -translate-y-1/2 rounded-full bg-stone-50" /><span className="absolute -right-3 top-1/2 size-6 -translate-y-1/2 rounded-full bg-stone-50" /><div className="border-b border-dashed border-stone-200 p-5 sm:p-6"><div className="flex items-start justify-between gap-4"><StatusBadge label={voucher.eligible ? "Có thể dùng" : "Chưa đủ điều kiện"} tone={voucher.eligible ? "success" : "warning"} /><span className="rounded-2xl bg-rose-50 p-3 text-rose-600"><TicketPercent /></span></div><h2 className="mt-4 text-xl font-black text-stone-950">{voucher.name}</h2><p className="mt-2 text-sm leading-6 text-stone-500">{voucher.description || discountDescription(voucher)}</p><div className="mt-5 grid gap-3 rounded-xl bg-stone-50 p-4 text-sm sm:grid-cols-2"><div><p className="text-xs font-bold uppercase text-stone-400">Mã voucher</p><p className="mt-1 font-black tracking-wide text-rose-600">{voucher.code}</p></div><div><p className="text-xs font-bold uppercase text-stone-400">Phạm vi</p><p className="mt-1 font-black text-stone-900">{scopeLabel(voucher.scope)}</p></div><div className="flex items-start gap-2 sm:col-span-2"><CalendarClock className="mt-0.5 size-4 text-stone-400" /><p className="text-stone-600">Hạn dùng {new Date(voucher.endsAt).toLocaleString("vi-VN")}</p></div></div>{voucher.ineligibleReason ? <p className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm font-semibold text-amber-800">{voucher.ineligibleReason}</p> : null}</div><div className="p-4"><Button className="w-full" size="lg" variant={voucher.eligible ? "default" : "outline"} onClick={() => void copy(voucher.code)}><Copy />{copied === voucher.code ? "Đã sao chép" : "Sao chép mã"}</Button></div></article>)}</div>}
  </div>;
}

function discountDescription(voucher: VoucherWalletItem) {
  if (voucher.discountMethod === "PERCENTAGE") return `Giảm ${(voucher.discountRateBps ?? 0) / 100}%${voucher.maxDiscountVnd ? `, tối đa ${voucher.maxDiscountVnd.toLocaleString("vi-VN")}đ` : ""}.`;
  return `Giảm ${(voucher.fixedDiscountVnd ?? 0).toLocaleString("vi-VN")}đ.`;
}

function scopeLabel(scope: string) {
  return ({ SHIPPING_DISCOUNT: "Phí giao hàng", ORDER_DISCOUNT: "Toàn đơn", PRODUCT_DISCOUNT: "Sản phẩm", PRODUCT_LIST_DISCOUNT: "Danh sách sản phẩm", CATEGORY_DISCOUNT: "Danh mục" } as Record<string, string>)[scope] ?? scope;
}
