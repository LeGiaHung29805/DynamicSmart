"use client";

import { useCallback, useEffect, useState } from "react";
import { Check, Clock3, Copy, History, ShoppingBag, Sparkles, TicketPercent } from "lucide-react";
import { useRouter } from "next/navigation";
import { EmptyState, ErrorState, LoadingState } from "@/components/common/PageState";
import { Price } from "@/components/common/Price";
import { Button } from "@/components/ui/Button";
import { useToast } from "@/components/ui/Toast";
import { isApiError } from "@/lib/api/error";
import { useAuthSession } from "@/lib/auth/session";
import { customerApi } from "../api/customer.api";
import type { VoucherUsageHistory, VoucherWalletItem } from "../types/customer.types";

type Tab = "wallet" | "history";

function discountLabel(voucher: VoucherWalletItem) {
  if (voucher.discountMethod === "PERCENTAGE") return `Giảm ${(voucher.discountRateBps ?? 0) / 100}%`;
  return `Giảm ${new Intl.NumberFormat("vi-VN").format(voucher.fixedDiscountVnd ?? 0)}₫`;
}

function scopeLabel(scope: string) {
  return ({ ORDER_DISCOUNT: "Toàn đơn", SHIPPING_DISCOUNT: "Phí vận chuyển", PRODUCT_DISCOUNT: "Sản phẩm", PRODUCT_LIST_DISCOUNT: "Nhóm sản phẩm", CATEGORY_DISCOUNT: "Danh mục" } as Record<string, string>)[scope] ?? scope;
}

export function VoucherWalletPage() {
  const router = useRouter();
  const session = useAuthSession();
  const [tab, setTab] = useState<Tab>("wallet");
  const [vouchers, setVouchers] = useState<VoucherWalletItem[]>([]);
  const [history, setHistory] = useState<VoucherUsageHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState("");
  const { showToast } = useToast();

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    const [walletResult, historyResult] = await Promise.allSettled([customerApi.vouchers(), customerApi.voucherHistory()]);
    if (walletResult.status === "fulfilled") setVouchers(walletResult.value);
    else setError(isApiError(walletResult.reason) ? walletResult.reason.message : "Không thể tải kho voucher.");
    if (historyResult.status === "fulfilled") setHistory(historyResult.value);
    setLoading(false);
  }, []);

  useEffect(() => {
    if (session.status !== "authenticated") return;
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load, session.status]);

  async function copyCode(code: string) {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(code);
      showToast(`Đã sao chép mã ${code}.`, "success");
      window.setTimeout(() => setCopied(""), 1600);
    } catch { showToast("Không thể sao chép tự động. Hãy chọn mã và sao chép.", "error"); }
  }

  return (
    <div className="mx-auto w-full max-w-7xl space-y-7 px-4 py-10 sm:px-6 lg:px-8">
      <div className="overflow-hidden rounded-[2rem] bg-gradient-to-br from-emerald-950 via-emerald-900 to-teal-700 px-6 py-9 text-white sm:px-9">
        <div className="flex flex-wrap items-end justify-between gap-6"><div><p className="text-xs font-black tracking-[0.2em] text-emerald-300 uppercase">Ưu đãi dành cho bạn</p><h1 className="mt-3 text-4xl font-black tracking-tight sm:text-5xl">Kho voucher</h1><p className="mt-3 max-w-2xl text-sm leading-6 text-emerald-100/80 sm:text-base">Xem điều kiện, sao chép mã và kiểm tra lịch sử voucher đã sử dụng.</p></div><span className="grid size-20 place-items-center rounded-3xl bg-white/10 ring-1 ring-white/15"><TicketPercent className="size-9 text-emerald-200" /></span></div>
      </div>

      <div className="flex w-fit rounded-2xl border border-slate-200 bg-white p-1.5 shadow-sm" role="tablist">
        <button className={`flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-bold transition ${tab === "wallet" ? "bg-emerald-800 text-white" : "text-slate-500 hover:bg-slate-50"}`} onClick={() => setTab("wallet")} role="tab" aria-selected={tab === "wallet"} type="button"><Sparkles className="size-4" />Voucher của tôi <span className={`rounded-full px-2 py-0.5 text-xs ${tab === "wallet" ? "bg-white/15" : "bg-slate-100"}`}>{vouchers.length}</span></button>
        <button className={`flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-bold transition ${tab === "history" ? "bg-emerald-800 text-white" : "text-slate-500 hover:bg-slate-50"}`} onClick={() => setTab("history")} role="tab" aria-selected={tab === "history"} type="button"><History className="size-4" />Lịch sử sử dụng</button>
      </div>

      {session.status === "loading" ? <LoadingState /> : session.status === "anonymous" ? <div className="rounded-3xl border border-slate-200 bg-white px-6 py-12 text-center shadow-sm"><TicketPercent className="mx-auto size-12 text-emerald-700" /><h2 className="mt-4 text-xl font-black text-slate-950">Đăng nhập để xem kho voucher</h2><p className="mt-2 text-sm text-slate-500">Voucher và lịch sử sử dụng được lưu riêng theo tài khoản.</p><Button className="mt-6 bg-emerald-800 text-white hover:bg-emerald-700" onClick={() => router.push("/login?returnTo=%2Fvouchers")}>Đăng nhập</Button></div> : loading ? <LoadingState /> : error && vouchers.length === 0 ? <ErrorState description={error} onAction={load} /> : tab === "wallet" ? (
        vouchers.length === 0 ? <EmptyState title="Chưa có voucher" description="Voucher phù hợp sẽ xuất hiện tại đây." /> : (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{vouchers.map((voucher) => (
            <article className="relative flex min-h-72 flex-col overflow-hidden rounded-3xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-1 hover:shadow-xl hover:shadow-slate-950/5" key={voucher.id}>
              <div className="absolute -top-10 -right-10 size-28 rounded-full bg-emerald-50" />
              <div className="relative flex items-start justify-between gap-3"><span className="grid size-11 place-items-center rounded-2xl bg-emerald-800 text-white"><TicketPercent className="size-5" /></span><span className={`rounded-full px-2.5 py-1 text-[11px] font-black ${voucher.eligible ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700"}`}>{voucher.eligible ? "Dùng được" : "Có điều kiện"}</span></div>
              <p className="mt-5 text-xs font-black tracking-[0.14em] text-emerald-700 uppercase">{scopeLabel(voucher.scope)}</p>
              <h2 className="mt-1 line-clamp-2 text-lg font-black text-slate-950">{voucher.name}</h2>
              <p className="mt-2 text-2xl font-black text-emerald-800">{discountLabel(voucher)}</p>
              <div className="mt-3 space-y-1 text-xs leading-5 text-slate-500"><p>{voucher.description || "Ưu đãi dành cho tài khoản của bạn"}</p><p className="flex items-center gap-1"><Clock3 className="size-3.5" />Hạn {new Date(voucher.endsAt).toLocaleDateString("vi-VN")}</p></div>
              {!voucher.eligible && voucher.ineligibleReason ? <p className="mt-2 line-clamp-2 text-xs text-amber-700">{voucher.ineligibleReason}</p> : null}
              <div className="mt-auto pt-5"><div className="flex items-center justify-between gap-2 rounded-2xl border border-dashed border-emerald-300 bg-emerald-50/60 p-2"><code className="truncate px-2 text-sm font-black text-emerald-900">{voucher.code}</code><Button aria-label={`Sao chép ${voucher.code}`} size="icon" variant="ghost" onClick={() => void copyCode(voucher.code)}>{copied === voucher.code ? <Check className="text-emerald-700" /> : <Copy />}</Button></div></div>
            </article>
          ))}</div>
        )
      ) : history.length === 0 ? <EmptyState title="Chưa sử dụng voucher" description="Voucher đã dùng khi đặt hàng sẽ được lưu tại đây." /> : (
        <div className="space-y-4">{history.map((item) => (
          <article className="flex flex-wrap items-center justify-between gap-5 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6" key={item.id}>
            <div className="flex min-w-0 items-center gap-4"><span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-slate-100 text-slate-600"><History className="size-5" /></span><div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><h2 className="font-black">{item.voucherName}</h2><code className="rounded-lg bg-emerald-50 px-2 py-1 text-xs font-bold text-emerald-800">{item.voucherCode}</code></div><p className="mt-1 flex items-center gap-1.5 text-xs text-slate-500"><ShoppingBag className="size-3.5" />{item.orderId ? `Đơn hàng ${item.orderId.slice(0, 8)} · ` : ""}{new Date(item.consumedAt ?? item.releasedAt ?? item.createdAt).toLocaleString("vi-VN")}</p></div></div>
            <div className="text-right"><p className="text-xs text-slate-500">Tổng ưu đãi</p><p className="mt-1 text-lg font-black text-emerald-700">-<Price value={item.discountAmountVnd + item.shippingDiscountVnd} /></p></div>
          </article>
        ))}</div>
      )}
    </div>
  );
}
