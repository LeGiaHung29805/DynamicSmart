"use client";

import { CalendarClock, Copy, History, TicketPercent } from "lucide-react";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/Button";
import { useAuthSession } from "@/lib/auth/session";
import { customerApi } from "../api/customer.api";
import type { VoucherUsageHistory, VoucherWalletItem } from "../types/customer.types";

export function VoucherWalletPage() {
  const session = useAuthSession();
  const [items, setItems] = useState<VoucherWalletItem[]>([]);
  const [history, setHistory] = useState<VoucherUsageHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [copied, setCopied] = useState("");
  const [copyError, setCopyError] = useState("");
  const [activeTab, setActiveTab] = useState<"wallet" | "history">("wallet");

  useEffect(() => {
    if (session.status !== "authenticated") return;
    let active = true;
    Promise.all([customerApi.vouchers(), customerApi.voucherHistory()])
      .then(([wallet, usageHistory]) => { if (active) { setItems(wallet); setHistory(usageHistory); setError(false); } })
      .catch(() => { if (active) setError(true); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [session.status]);

  const copy = async (code: string) => {
    try {
      await navigator.clipboard.writeText(code);
      setCopyError(""); setCopied(code); window.setTimeout(() => setCopied(""), 1800);
    } catch { setCopyError("Không thể sao chép tự động. Hãy bôi đen mã và sao chép thủ công."); }
  };

  return <div className="space-y-7"><PageHeader eyebrow="Ưu đãi dành cho bạn" title="Kho voucher" description="Xem voucher đang có hoặc chuyển sang lịch sử để theo dõi những mã đã sử dụng." />
    <div aria-label="Nội dung voucher" className="inline-flex w-full rounded-2xl border border-stone-200 bg-white p-1.5 shadow-sm sm:w-auto" role="tablist">
      <button aria-controls="voucher-wallet-panel" aria-selected={activeTab === "wallet"} className={`flex flex-1 items-center justify-center gap-2 rounded-xl px-5 py-3 text-sm font-black transition sm:flex-none ${activeTab === "wallet" ? "bg-rose-600 text-white shadow-sm" : "text-stone-600 hover:bg-stone-100 hover:text-stone-950"}`} onClick={() => setActiveTab("wallet")} role="tab" type="button"><TicketPercent className="size-4" />Voucher của tôi<span className={`rounded-full px-2 py-0.5 text-xs ${activeTab === "wallet" ? "bg-white/20" : "bg-stone-100"}`}>{items.length}</span></button>
      <button aria-controls="voucher-history-panel" aria-selected={activeTab === "history"} className={`flex flex-1 items-center justify-center gap-2 rounded-xl px-5 py-3 text-sm font-black transition sm:flex-none ${activeTab === "history" ? "bg-rose-600 text-white shadow-sm" : "text-stone-600 hover:bg-stone-100 hover:text-stone-950"}`} onClick={() => setActiveTab("history")} role="tab" type="button"><History className="size-4" />Lịch sử sử dụng<span className={`rounded-full px-2 py-0.5 text-xs ${activeTab === "history" ? "bg-white/20" : "bg-stone-100"}`}>{history.length}</span></button>
    </div>

    {session.status === "anonymous" ? <SurfacePanel className="text-amber-700">Vui lòng đăng nhập để xem voucher của bạn.</SurfacePanel> : loading ? <SurfacePanel>Đang tải voucher…</SurfacePanel> : error ? <SurfacePanel className="text-red-600">Không thể tải dữ liệu voucher.</SurfacePanel> : activeTab === "wallet" ? <div className="space-y-7" id="voucher-wallet-panel" role="tabpanel">
      <div className="overflow-hidden rounded-3xl bg-gradient-to-r from-rose-700 via-rose-600 to-orange-500 px-6 py-8 text-white shadow-lg sm:px-10"><p className="text-sm font-bold uppercase tracking-[0.2em] text-rose-100">DynamicMart Rewards</p><h2 className="mt-2 text-3xl font-black tracking-tight">Ưu đãi dành riêng cho bạn</h2><p className="mt-3 max-w-2xl text-sm leading-6 text-rose-50">Voucher mặc định và voucher được cấp riêng sẽ xuất hiện tại đây. Mã chưa đủ điều kiện vẫn hiển thị kèm lý do.</p></div>
      {copyError ? <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">{copyError}</p> : null}
      {items.length === 0 ? <SurfacePanel>Ví voucher hiện đang trống.</SurfacePanel> : <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">{items.map((voucher) => <article className={`relative flex min-w-0 flex-col overflow-hidden rounded-2xl border bg-white shadow-sm ${voucher.eligible ? "border-stone-200" : "border-amber-200"}`} key={voucher.id}><span className="absolute -left-3 top-1/2 size-6 -translate-y-1/2 rounded-full bg-stone-50" /><span className="absolute -right-3 top-1/2 size-6 -translate-y-1/2 rounded-full bg-stone-50" /><div className="flex-1 border-b border-dashed border-stone-200 p-5"><div className="flex items-start justify-between gap-3"><StatusBadge label={voucher.eligible ? "Có thể dùng" : "Chưa đủ điều kiện"} tone={voucher.eligible ? "success" : "warning"} /><span className="rounded-2xl bg-rose-50 p-3 text-rose-600"><TicketPercent /></span></div><h2 className="mt-4 text-lg font-black text-stone-950">{voucher.name}</h2><p className="mt-2 text-sm leading-6 text-stone-500">{voucher.description || discountDescription(voucher)}</p><div className="mt-5 grid gap-3 rounded-xl bg-stone-50 p-4 text-sm"><div><p className="text-xs font-bold uppercase text-stone-400">Mã voucher</p><p className="mt-1 truncate font-black tracking-wide text-rose-600">{voucher.code}</p></div><div><p className="text-xs font-bold uppercase text-stone-400">Phạm vi</p><p className="mt-1 font-black text-stone-900">{scopeLabel(voucher.scope)}</p></div><div className="flex items-start gap-2"><CalendarClock className="mt-0.5 size-4 shrink-0 text-stone-400" /><p className="text-stone-600">Hạn dùng {new Date(voucher.endsAt).toLocaleDateString("vi-VN")}</p></div></div>{voucher.ineligibleReason ? <p className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm font-semibold text-amber-800">{voucher.ineligibleReason}</p> : null}</div><div className="p-4"><Button className="w-full" size="lg" variant={voucher.eligible ? "default" : "outline"} onClick={() => void copy(voucher.code)}><Copy />{copied === voucher.code ? "Đã sao chép" : "Sao chép mã"}</Button></div></article>)}</div>}
    </div> : <div id="voucher-history-panel" role="tabpanel"><SurfacePanel className="overflow-hidden p-0">
      <div className="flex items-center gap-3 border-b border-stone-200 px-5 py-4 sm:px-6"><span className="rounded-xl bg-slate-100 p-2 text-slate-700"><History className="size-5" /></span><div><h2 className="font-black text-stone-950">Lịch sử sử dụng voucher</h2><p className="text-sm text-stone-500">Theo dõi voucher đã dùng, đang giữ hoặc đã được hoàn lượt.</p></div></div>
      {history.length === 0 ? <p className="px-5 py-8 text-sm text-stone-500 sm:px-6">Bạn chưa có lịch sử sử dụng voucher.</p> : <div className="divide-y divide-stone-100">{history.map((entry) => <div className="grid gap-3 px-5 py-4 sm:grid-cols-[1fr_auto] sm:items-center sm:px-6" key={entry.id}><div><div className="flex flex-wrap items-center gap-2"><p className="font-black text-stone-950">{entry.voucherName}</p>{entry.voucherCode ? <code className="rounded bg-rose-50 px-2 py-1 text-xs font-black text-rose-700">{entry.voucherCode}</code> : null}<StatusBadge label={historyStatus(entry.status).label} tone={historyStatus(entry.status).tone} /></div><p className="mt-1 text-sm text-stone-500">{historyDate(entry)}{entry.orderId ? ` · Đơn #${entry.orderId.slice(0, 8).toUpperCase()}` : ""}</p>{entry.releaseReason ? <p className="mt-1 text-xs text-stone-400">Lý do: {releaseReason(entry.releaseReason)}</p> : null}</div><p className="font-black text-emerald-700">-{(entry.discountAmountVnd + entry.shippingDiscountVnd).toLocaleString("vi-VN")}đ</p></div>)}</div>}
    </SurfacePanel></div>}
  </div>;
}

function discountDescription(voucher: VoucherWalletItem) {
  if (voucher.discountMethod === "PERCENTAGE") return `Giảm ${(voucher.discountRateBps ?? 0) / 100}%${voucher.maxDiscountVnd ? `, tối đa ${voucher.maxDiscountVnd.toLocaleString("vi-VN")}đ` : ""}.`;
  return `Giảm ${(voucher.fixedDiscountVnd ?? 0).toLocaleString("vi-VN")}đ.`;
}

function scopeLabel(scope: string) {
  return ({ SHIPPING_DISCOUNT: "Phí giao hàng", ORDER_DISCOUNT: "Toàn đơn", PRODUCT_DISCOUNT: "Sản phẩm", PRODUCT_LIST_DISCOUNT: "Danh sách sản phẩm", CATEGORY_DISCOUNT: "Danh mục" } as Record<string, string>)[scope] ?? scope;
}

function historyStatus(status: VoucherUsageHistory["status"]): { label: string; tone: "neutral" | "success" | "warning" | "danger" } {
  switch (status) {
    case "CONSUMED": return { label: "Đã sử dụng", tone: "success" };
    case "RESERVED": return { label: "Đang giữ", tone: "warning" };
    case "RELEASED": return { label: "Đã hoàn lượt", tone: "neutral" };
    case "EXPIRED": return { label: "Hết thời gian giữ", tone: "danger" };
  }
}

function historyDate(entry: VoucherUsageHistory) {
  const value = entry.consumedAt ?? entry.releasedAt ?? entry.createdAt;
  return new Date(value).toLocaleString("vi-VN");
}

function releaseReason(reason: string) {
  return ({ PAYMENT_FAILED: "Thanh toán thất bại", RESERVATION_EXPIRED: "Hết thời gian giữ voucher" } as Record<string, string>)[reason] ?? reason;
}
