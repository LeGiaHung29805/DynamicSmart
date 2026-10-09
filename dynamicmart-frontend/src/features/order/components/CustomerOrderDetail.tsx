"use client";

/* eslint-disable @next/next/no-img-element -- Order snapshots preserve external Catalog image URLs. */

import Link from "next/link";
import { ArrowLeft, CheckCircle2, Clock3, CreditCard, MapPin, PackageCheck, RefreshCw, Star, Truck } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { ErrorState, LoadingState } from "@/components/common/PageState";
import { StatusBadge } from "@/components/common/StatusBadge";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { CreateReviewModal } from "@/features/engagement";
import { customerPaymentsApi, type CustomerPayment } from "@/features/payment";
import { paymentStatusLabel, paymentStatusTone } from "@/features/payment/utils/payment-format";
import { isApiError } from "@/lib/api/error";
import { customerOrdersApi, type CustomerOrder, type CustomerOrderItem } from "../api/customer-orders.api";
import { formatDateTime, formatVnd, orderActorLabel, orderStatusLabel, orderStatusTone, paymentMethodLabel, paymentTimingLabel } from "../utils/order-format";

export function CustomerOrderDetail({ orderId }: Readonly<{ orderId: string }>) {
  const [order, setOrder] = useState<CustomerOrder | null>(null);
  const [payment, setPayment] = useState<CustomerPayment | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reviewItem, setReviewItem] = useState<CustomerOrderItem | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const nextOrder = await customerOrdersApi.get(orderId);
      setOrder(nextOrder);
      if (nextOrder.paymentMethod === "FREE") {
        setPayment(null);
      } else {
        try {
          setPayment(await customerPaymentsApi.getByOrder(orderId));
        } catch (cause) {
          if (!isApiError(cause) || cause.status !== 404) throw cause;
          setPayment(null);
        }
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tải đơn hàng.");
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  async function confirmReceived() {
    setSubmitting(true);
    setError(null);
    try {
      setOrder(await customerOrdersApi.confirmReceived(orderId));
      if (payment?.method === "COD") setPayment(await customerPaymentsApi.getByOrder(orderId));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể xác nhận đã nhận hàng.");
    } finally {
      setSubmitting(false);
    }
  }

  async function continuePayment() {
    setSubmitting(true);
    setError(null);
    try {
      const nextPayment = await customerPaymentsApi.createAttempt(orderId);
      setPayment(nextPayment);
      if (!nextPayment.redirectUrl) throw new Error("Cổng thanh toán chưa trả về đường dẫn hợp lệ.");
      window.location.assign(nextPayment.redirectUrl);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể mở cổng thanh toán.");
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <main className="mx-auto min-h-[60vh] w-full max-w-6xl px-4 py-12 sm:px-6"><LoadingState title="Đang tải đơn hàng" /></main>;
  if (!order) return <main className="mx-auto min-h-[60vh] w-full max-w-3xl px-4 py-12 sm:px-6"><ErrorState description={error ?? "Không tìm thấy đơn hàng."} onAction={() => void load()} /></main>;

  const canContinuePayment = payment?.status === "PENDING" && payment.method !== "COD"
    && (payment.timing === "PREPAID" || order.status === "HANDOVER_PENDING" || order.status === "DELIVERED");

  return (
    <main className="mx-auto min-h-[60vh] w-full max-w-6xl px-4 py-10 sm:px-6">
      <Link className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-brand" href="/customer/account/orders"><ArrowLeft className="size-4" />Quay lại danh sách đơn hàng</Link>
      <div className="mt-6 overflow-hidden rounded-[2rem] bg-slate-950 px-6 py-7 text-white shadow-xl sm:px-8">
        <div className="flex flex-wrap items-start justify-between gap-5"><div><p className="text-xs font-black uppercase tracking-[0.18em] text-rose-300">Chi tiết đơn hàng</p><h1 className="mt-2 text-3xl font-black">{order.orderNumber}</h1><p className="mt-2 text-sm text-slate-300">Đặt lúc {formatDateTime(order.createdAt)}</p></div><StatusBadge label={orderStatusLabel(order.status)} tone={orderStatusTone(order.status)} /></div>
      </div>
      {error ? <div className="mt-5 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div> : null}

      <div className="mt-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
        <div className="space-y-6">
          <SurfacePanel>
            <div className="flex items-center justify-between gap-4"><div><p className="text-xs font-black uppercase tracking-wider text-brand">Sản phẩm</p><h2 className="mt-1 text-xl font-black">Các mặt hàng đã đặt</h2></div><span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-600">{order.items.length} mặt hàng</span></div>
            <div className="mt-5 divide-y divide-slate-100 rounded-2xl border border-slate-200">{order.items.map((item) => <div className="flex flex-wrap items-center justify-between gap-4 p-4" key={item.itemId}><div className="flex min-w-0 items-center gap-3">{item.imageUrl ? <img alt={item.productName} className="size-16 shrink-0 rounded-xl border border-slate-200 object-cover" src={item.imageUrl} /> : <div className="grid size-16 shrink-0 place-items-center rounded-xl bg-slate-100 text-xs font-bold text-slate-400">Ảnh</div>}<div className="min-w-0"><p className="truncate font-bold text-slate-950">{item.productName}</p><p className="mt-0.5 text-xs text-slate-500">{item.variantName || item.sku}</p><p className="mt-1 text-sm text-slate-600">{formatVnd(item.unitPriceVnd)} × {item.quantity}</p></div></div><div className="text-right"><p className="font-black text-slate-950">{formatVnd(item.lineTotalVnd)}</p>{order.status === "COMPLETED" ? <Button className="mt-2 gap-1.5" size="sm" variant="outline" onClick={() => setReviewItem(item)}><Star className="size-3.5 fill-amber-400 text-amber-500" />Đánh giá</Button> : null}</div></div>)}</div>
          </SurfacePanel>

          <SurfacePanel>
            <div className="flex items-start gap-3"><MapPin className="mt-0.5 size-5 text-brand" /><div><h2 className="font-black text-slate-950">Thông tin nhận hàng</h2><p className="mt-2 font-semibold">{order.address.recipientName} · {order.address.phone}</p><p className="mt-1 text-sm leading-6 text-slate-600">{order.address.addressLine}, {order.address.wardName}, {order.address.provinceName}</p></div></div>
            <div className="mt-5 flex items-start gap-3 border-t border-slate-100 pt-5"><Truck className="mt-0.5 size-5 text-brand" /><div><h3 className="font-bold">{order.shipping.serviceName}</h3><p className="mt-1 text-sm text-slate-600">Đơn vị: {order.shipping.provider} · Phí: {formatVnd(order.shipping.payableFeeVnd)}</p><p className="mt-1 text-sm text-slate-500">Dự kiến: {order.shipping.etaText || formatDateTime(order.shipping.eta)}</p></div></div>
          </SurfacePanel>

          <SurfacePanel>
            <div className="flex items-center gap-3"><Clock3 className="size-5 text-brand" /><h2 className="font-black text-slate-950">Lịch sử xử lý</h2></div>
            <ol className="mt-5 space-y-4">{order.timeline.map((entry) => <li className="relative border-l-2 border-rose-200 pb-1 pl-5" key={entry.historyId}><span className="absolute -left-[7px] top-1 size-3 rounded-full bg-rose-500 ring-4 ring-white" /><p className="font-bold">{orderStatusLabel(entry.toStatus)}</p><p className="mt-1 text-xs text-slate-500">{formatDateTime(entry.createdAt)} · {orderActorLabel(entry.actorType)}</p>{entry.reason ? <p className="mt-1 text-sm text-slate-600">{entry.reason}</p> : null}</li>)}</ol>
          </SurfacePanel>
        </div>

        <aside className="space-y-6">
          <SurfacePanel><h2 className="font-black text-slate-950">Tóm tắt thanh toán</h2><dl className="mt-4 space-y-3 text-sm"><MoneyRow label="Giá niêm yết" value={order.money.itemsListSubtotalVnd} /><MoneyRow label="Giảm giá trực tiếp" value={-order.money.directSaleDiscountVnd} /><MoneyRow label="Mã giảm sản phẩm/đơn" value={-(order.money.productDiscountVnd + order.money.orderDiscountVnd)} /><MoneyRow label="Phí giao hàng" value={order.money.shippingFeeVnd} /><MoneyRow label="Giảm phí giao hàng" value={-order.money.shippingDiscountVnd} /><div className="flex justify-between border-t border-slate-200 pt-4 text-base font-black"><dt>Tổng thanh toán</dt><dd className="text-brand">{formatVnd(order.money.finalTotalVnd)}</dd></div></dl></SurfacePanel>

          <SurfacePanel>
            <div className="flex items-center gap-3"><CreditCard className="size-5 text-brand" /><h2 className="font-black text-slate-950">Thanh toán</h2></div>
            <dl className="mt-4 space-y-3 text-sm"><InfoRow label="Phương thức" value={paymentMethodLabel(order.paymentMethod)} /><InfoRow label="Thời điểm" value={paymentTimingLabel(order.paymentTiming)} />{payment ? <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">Trạng thái</dt><dd><StatusBadge label={paymentStatusLabel(payment.status)} tone={paymentStatusTone(payment.status)} /></dd></div> : null}{payment?.paidAt ? <InfoRow label="Đã thanh toán lúc" value={formatDateTime(payment.paidAt)} /> : null}</dl>
            {canContinuePayment ? <Button className="mt-5 w-full" disabled={submitting} onClick={() => void continuePayment()}><CreditCard />{submitting ? "Đang mở cổng thanh toán…" : payment?.redirectUrl ? "Tiếp tục thanh toán" : "Tạo đường dẫn thanh toán"}</Button> : null}
            {payment?.status === "PENDING" && payment.timing === "POSTPAID" && order.status !== "HANDOVER_PENDING" && order.status !== "DELIVERED" ? <p className="mt-4 rounded-xl bg-amber-50 p-3 text-xs leading-5 text-amber-800">Thanh toán trả sau sẽ được mở khi quản trị viên chuyển đơn sang bước chờ bàn giao.</p> : null}
          </SurfacePanel>

          {order.availableActions.includes("CONFIRM_RECEIVED") ? <section className="rounded-3xl border border-emerald-200 bg-emerald-50 p-5"><div className="flex gap-3"><PackageCheck className="mt-0.5 size-6 text-emerald-700" /><div><h2 className="font-black text-emerald-950">Bạn đã nhận được hàng?</h2><p className="mt-1 text-sm leading-6 text-emerald-800">Chỉ xác nhận sau khi đã nhận và kiểm tra kiện hàng. Với COD, hệ thống đồng thời ghi nhận đã thanh toán.</p></div></div><Button className="mt-4 w-full" disabled={submitting} onClick={() => void confirmReceived()}>{submitting ? "Đang xác nhận…" : "Tôi đã nhận hàng"}</Button></section> : null}
          {order.status === "COMPLETED" ? <section className="flex gap-3 rounded-3xl border border-emerald-200 bg-emerald-50 p-5 text-emerald-900"><CheckCircle2 className="size-6 shrink-0 text-emerald-600" /><div><p className="font-black">Đơn hàng đã hoàn tất.</p><p className="mt-1 text-xs leading-5">Cảm ơn bạn đã mua sắm. Bạn có thể đánh giá từng sản phẩm trong đơn.</p></div></section> : null}
          <Button className="w-full" variant="outline" onClick={() => void load()}><RefreshCw />Làm mới trạng thái</Button>
        </aside>
      </div>

      <CreateReviewModal isOpen={!!reviewItem} orderItemId={reviewItem?.itemId} productName={reviewItem?.productName} onClose={() => setReviewItem(null)} />
    </main>
  );
}

function MoneyRow({ label, value }: Readonly<{ label: string; value: number }>) {
  return <div className="flex justify-between gap-4"><dt className="text-slate-500">{label}</dt><dd className={value < 0 ? "font-semibold text-emerald-700" : "font-semibold"}>{value < 0 ? `−${formatVnd(Math.abs(value))}` : formatVnd(value)}</dd></div>;
}

function InfoRow({ label, value }: Readonly<{ label: string; value: string }>) {
  return <div className="flex justify-between gap-4"><dt className="text-slate-500">{label}</dt><dd className="text-right font-semibold">{value}</dd></div>;
}
