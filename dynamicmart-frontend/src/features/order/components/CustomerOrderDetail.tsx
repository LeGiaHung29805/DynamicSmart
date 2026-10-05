"use client";

import Link from "next/link";
import { ArrowLeft, CheckCircle2, PackageCheck } from "lucide-react";
import { useEffect, useState } from "react";
import { ErrorState, LoadingState } from "@/components/common/PageState";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/Button";
import { customerOrdersApi, type CustomerOrder } from "../api/customer-orders.api";

const money = (value: number) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value);

export function CustomerOrderDetail({ orderId }: Readonly<{ orderId: string }>) {
  const [order, setOrder] = useState<CustomerOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setLoading(true); setError(null);
    try { setOrder(await customerOrdersApi.get(orderId)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Không thể tải đơn hàng."); }
    finally { setLoading(false); }
  }
  useEffect(() => {
    let ignored = false;
    customerOrdersApi.get(orderId)
      .then((nextOrder) => { if (!ignored) setOrder(nextOrder); })
      .catch((cause) => { if (!ignored) setError(cause instanceof Error ? cause.message : "Không thể tải đơn hàng."); })
      .finally(() => { if (!ignored) setLoading(false); });
    return () => { ignored = true; };
  }, [orderId]);

  async function confirmReceived() {
    setSubmitting(true); setError(null);
    try { setOrder(await customerOrdersApi.confirmReceived(orderId)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Không thể xác nhận đã nhận hàng."); }
    finally { setSubmitting(false); }
  }

  if (loading) return <main className="mx-auto min-h-[60vh] w-full max-w-3xl px-4 py-12 sm:px-6"><LoadingState title="Đang tải đơn hàng" /></main>;
  if (!order) return <main className="mx-auto min-h-[60vh] w-full max-w-3xl px-4 py-12 sm:px-6"><ErrorState description={error ?? "Không tìm thấy đơn hàng."} onAction={() => void load()} /></main>;

  return <main className="mx-auto min-h-[60vh] w-full max-w-3xl px-4 py-12 sm:px-6"><Link className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-brand" href="/"><ArrowLeft className="size-4" />Về trang chủ</Link><SurfacePanel className="mt-6"><div className="flex items-start justify-between gap-4"><div><p className="text-xs font-black tracking-wider text-brand uppercase">Đơn hàng</p><h1 className="mt-2 text-2xl font-black text-slate-950">{order.orderNumber}</h1></div><StatusBadge label={order.status} tone={order.status === "COMPLETED" ? "success" : "warning"} /></div><dl className="mt-6 grid grid-cols-2 gap-4 text-sm"><dt className="text-slate-500">Phương thức</dt><dd className="font-semibold">{order.paymentMethod}</dd><dt className="text-slate-500">Tổng thanh toán</dt><dd className="font-semibold">{money(order.finalTotalVnd)}</dd></dl>{order.canConfirmReceived ? <div className="mt-8 rounded-2xl border border-emerald-200 bg-emerald-50 p-5"><div className="flex gap-3"><PackageCheck className="mt-0.5 size-6 text-emerald-700" /><div><h2 className="font-bold text-emerald-950">Bạn đã nhận được hàng?</h2><p className="mt-1 text-sm text-emerald-800">Với đơn COD, thao tác này đồng thời xác nhận đã thanh toán và hoàn tất đơn.</p></div></div><Button className="mt-4" disabled={submitting} onClick={() => void confirmReceived()}>{submitting ? "Đang xác nhận…" : "Đã nhận hàng"}</Button></div> : null}{order.status === "COMPLETED" ? <div className="mt-8 flex items-center gap-3 rounded-2xl bg-emerald-50 p-5 text-emerald-900"><CheckCircle2 className="size-6" /><span className="font-bold">Đơn hàng đã hoàn tất.</span></div> : null}{error ? <p className="mt-4 text-sm text-danger">{error}</p> : null}</SurfacePanel></main>;
}
