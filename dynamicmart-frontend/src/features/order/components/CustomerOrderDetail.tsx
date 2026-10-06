"use client";

import Image from "next/image";
import Link from "next/link";
import { ArrowLeft, CheckCircle2, PackageCheck, ReceiptText, Truck } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { ErrorState, LoadingState } from "@/components/common/PageState";
import { StatusBadge } from "@/components/common/StatusBadge";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { isApiError } from "@/lib/api/error";
import { useAuthSession } from "@/lib/auth/session";
import { customerOrdersApi } from "../api/customer-orders.api";
import type { OrderDetail } from "../types/order.types";
import { formatDateTime, formatVnd, orderStatusLabel, orderStatusTone } from "../utils/order-format";

export function CustomerOrderDetail({ orderId }: Readonly<{ orderId: string }>) {
  const router = useRouter();
  const session = useAuthSession();
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const receiveKey = useRef<string | null>(null);

  async function load() {
    setLoading(true);
    setError("");
    try {
      const [detail, timeline] = await Promise.all([customerOrdersApi.get(orderId), customerOrdersApi.timeline(orderId)]);
      setOrder({ ...detail, timeline: timeline.timeline });
    } catch (cause) {
      setError(isApiError(cause) ? cause.message : "Không thể tải đơn hàng.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (session.status === "loading") return;
    if (session.status === "anonymous") {
      router.replace(`/login?returnTo=${encodeURIComponent(`/customer/account/orders/${orderId}`)}`);
      return;
    }
    let ignored = false;
    Promise.all([customerOrdersApi.get(orderId), customerOrdersApi.timeline(orderId)])
      .then(([detail, timeline]) => { if (!ignored) setOrder({ ...detail, timeline: timeline.timeline }); })
      .catch((cause) => { if (!ignored) setError(isApiError(cause) ? cause.message : "Không thể tải đơn hàng."); })
      .finally(() => { if (!ignored) setLoading(false); });
    return () => { ignored = true; };
  }, [orderId, router, session.status]);

  async function confirmReceived() {
    if (submitting) return;
    setSubmitting(true);
    setError("");
    receiveKey.current ??= crypto.randomUUID();
    try {
      await customerOrdersApi.confirmReceived(orderId, receiveKey.current);
      receiveKey.current = null;
      await load();
    } catch (cause) {
      setError(isApiError(cause) ? cause.message : "Không thể xác nhận đã nhận hàng.");
    } finally {
      setSubmitting(false);
    }
  }

  if (session.status === "anonymous") return null;
  if (loading || session.status === "loading") return <LoadingState title="Đang tải đơn hàng" />;
  if (!order) return <ErrorState description={error || "Không tìm thấy đơn hàng."} onAction={() => void load()} />;

  return <div className="space-y-6"><Link className="inline-flex items-center gap-2 text-sm font-semibold text-stone-500 hover:text-brand" href="/customer/account/orders"><ArrowLeft className="size-4" />Danh sách đơn hàng</Link>
    <SurfacePanel><div className="flex flex-wrap items-start justify-between gap-4"><div><p className="text-xs font-black tracking-wider text-brand uppercase">Đơn hàng</p><h1 className="mt-2 text-2xl font-black text-stone-950">{order.orderNumber}</h1><p className="mt-2 text-sm text-stone-500">Tạo lúc {formatDateTime(order.createdAt)}</p></div><StatusBadge label={orderStatusLabel(order.status)} tone={orderStatusTone(order.status)} /></div>{error ? <p className="mt-5 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-danger">{error}</p> : null}</SurfacePanel>

    <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_340px]"><div className="space-y-6"><SurfacePanel><div className="flex items-center gap-2"><ReceiptText className="size-5 text-brand" /><h2 className="font-black">Sản phẩm</h2></div><div className="mt-5 space-y-4">{order.items.map((item) => <article className="grid grid-cols-[72px_minmax(0,1fr)_auto] gap-4 border-t border-stone-100 pt-4 first:border-0 first:pt-0" key={item.itemId}><div className="relative aspect-square overflow-hidden rounded-xl bg-stone-100">{item.imageUrl ? <Image alt={item.productName} className="object-cover" fill sizes="72px" src={item.imageUrl} unoptimized /> : null}</div><div><h3 className="font-bold text-stone-900">{item.productName}</h3><p className="mt-1 text-xs text-stone-500">{item.variantName || item.sku} · SL {item.quantity}</p><p className="mt-1 text-xs text-stone-500">Đơn giá {formatVnd(item.unitPriceVnd)}</p></div><strong className="text-right text-stone-900">{formatVnd(item.lineTotalVnd)}</strong></article>)}</div></SurfacePanel>

      <SurfacePanel><div className="flex items-center gap-2"><Truck className="size-5 text-brand" /><h2 className="font-black">Giao hàng</h2></div><div className="mt-5 grid gap-5 text-sm sm:grid-cols-2"><div><p className="font-bold text-stone-900">{order.address.recipientName} · {order.address.phone}</p><p className="mt-2 leading-6 text-stone-600">{order.address.addressLine}, {order.address.wardName}, {order.address.provinceName}</p></div><div><p className="font-bold text-stone-900">{order.shipping.provider} · {order.shipping.serviceName}</p><p className="mt-2 text-stone-600">Dự kiến: {order.shipping.etaText || formatDateTime(order.shipping.eta)}</p><p className="mt-1 text-stone-600">Phí phải trả: {formatVnd(order.shipping.payableFeeVnd)}</p></div></div></SurfacePanel>

      <SurfacePanel><h2 className="font-black">Lịch sử trạng thái</h2><ol className="mt-5 space-y-4">{order.timeline.map((entry) => <li className="relative border-l-2 border-rose-200 pl-5" key={entry.historyId}><span className="absolute -left-[7px] top-1 size-3 rounded-full bg-rose-500 ring-4 ring-white" /><div className="flex flex-wrap items-center justify-between gap-2"><strong>{orderStatusLabel(entry.toStatus)}</strong><time className="text-xs text-stone-500">{formatDateTime(entry.createdAt)}</time></div>{entry.reason ? <p className="mt-1 text-sm text-stone-600">{entry.reason}</p> : null}</li>)}</ol></SurfacePanel></div>

      <div className="space-y-6"><SurfacePanel><h2 className="font-black">Thanh toán</h2><dl className="mt-4 grid grid-cols-2 gap-3 text-sm"><dt className="text-stone-500">Phương thức</dt><dd className="font-semibold">{order.paymentMethod}</dd><dt className="text-stone-500">Thời điểm</dt><dd className="font-semibold">{order.paymentTiming}</dd><dt className="text-stone-500">Đã thanh toán</dt><dd>{formatDateTime(order.paymentSucceededAt)}</dd><dt className="text-stone-500">Hạn thanh toán</dt><dd>{formatDateTime(order.paymentDueAt)}</dd></dl></SurfacePanel>

        <SurfacePanel><h2 className="font-black">Tổng tiền</h2><div className="mt-4 space-y-3 text-sm"><MoneyRow label="Giá niêm yết" value={order.money.itemsListSubtotalVnd} /><MoneyRow label="Direct sale" value={-order.money.directSaleDiscountVnd} /><MoneyRow label="Voucher sản phẩm/đơn" value={-(order.money.productDiscountVnd + order.money.orderDiscountVnd)} /><MoneyRow label="Phí giao hàng" value={order.money.shippingFeeVnd} /><MoneyRow label="Giảm phí giao hàng" value={-order.money.shippingDiscountVnd} /><div className="flex justify-between border-t pt-3 text-base font-black"><span>Tổng cộng</span><span className="text-brand">{formatVnd(order.money.finalTotalVnd)}</span></div></div></SurfacePanel>

        {order.availableActions.includes("CONFIRM_RECEIVED") ? <SurfacePanel className="border-emerald-200 bg-emerald-50"><div className="flex gap-3"><PackageCheck className="mt-0.5 size-6 text-emerald-700" /><div><h2 className="font-bold text-emerald-950">Bạn đã nhận được hàng?</h2><p className="mt-1 text-sm text-emerald-800">Thao tác được xác minh lại ở máy chủ và có chống xử lý trùng.</p></div></div><Button className="mt-4" disabled={submitting} onClick={() => void confirmReceived()}>{submitting ? "Đang xác nhận…" : "Đã nhận hàng"}</Button></SurfacePanel> : null}
        {order.status === "COMPLETED" ? <SurfacePanel className="bg-emerald-50"><div className="flex items-center gap-3 text-emerald-900"><CheckCircle2 className="size-6" /><strong>Đơn hàng đã hoàn tất.</strong></div></SurfacePanel> : null}
        {order.status === "CANCELLED" ? <SurfacePanel className="border-red-200 bg-red-50"><strong className="text-red-800">Đơn đã hủy</strong><p className="mt-2 text-sm text-red-700">{order.cancelReason || "Không có lý do bổ sung."}</p></SurfacePanel> : null}
      </div></div>
  </div>;
}

function MoneyRow({ label, value }: Readonly<{ label: string; value: number }>) {
  return <div className="flex justify-between gap-3"><span className="text-stone-500">{label}</span><span>{value < 0 ? `−${formatVnd(Math.abs(value))}` : formatVnd(value)}</span></div>;
}
