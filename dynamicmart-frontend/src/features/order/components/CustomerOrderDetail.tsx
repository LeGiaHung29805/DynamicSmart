"use client";

import Link from "next/link";
import { ArrowLeft, CheckCircle2, PackageCheck, Star } from "lucide-react";
import { useEffect, useState } from "react";
import { ErrorState, LoadingState } from "@/components/common/PageState";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/Button";
import { CreateReviewModal } from "@/features/engagement";
import { customerOrdersApi, type CustomerOrder, type CustomerOrderItem } from "../api/customer-orders.api";

const money = (value: number) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value);

export function CustomerOrderDetail({ orderId }: Readonly<{ orderId: string }>) {
  const [order, setOrder] = useState<CustomerOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reviewItem, setReviewItem] = useState<CustomerOrderItem | null>(null);

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

  return (
    <main className="mx-auto min-h-[60vh] w-full max-w-3xl px-4 py-12 sm:px-6">
      <Link className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-brand" href="/">
        <ArrowLeft className="size-4" />Về trang chủ
      </Link>
      <SurfacePanel className="mt-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-black tracking-wider text-brand uppercase">Đơn hàng</p>
            <h1 className="mt-2 text-2xl font-black text-slate-950">{order.orderNumber}</h1>
          </div>
          <StatusBadge label={order.status} tone={order.status === "COMPLETED" ? "success" : "warning"} />
        </div>

        <dl className="mt-6 grid grid-cols-2 gap-4 text-sm border-b border-slate-100 pb-6">
          <div>
            <dt className="text-slate-500">Phương thức</dt>
            <dd className="font-semibold text-slate-900 mt-0.5">{order.paymentMethod}</dd>
          </div>
          <div>
            <dt className="text-slate-500">Tổng thanh toán</dt>
            <dd className="font-semibold text-emerald-700 mt-0.5">{money(order.money.finalTotalVnd)}</dd>
          </div>
        </dl>

        {order.items && order.items.length > 0 ? (
          <div className="mt-6 space-y-4">
            <h2 className="text-xs font-bold tracking-wider text-slate-400 uppercase">
              Sản phẩm trong đơn ({order.items.length})
            </h2>
            <div className="divide-y divide-slate-100 rounded-xl border border-slate-200">
              {order.items.map((item) => (
                <div className="flex flex-wrap items-center justify-between gap-4 p-4" key={item.itemId}>
                  <div className="flex items-center gap-3 min-w-0">
                    {item.imageUrl ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img
                        alt={item.productName}
                        className="size-14 rounded-lg border border-slate-200 object-cover shrink-0"
                        src={item.imageUrl}
                      />
                    ) : (
                      <div className="grid size-14 shrink-0 place-items-center rounded-lg bg-slate-100 text-xs text-slate-400 font-bold">
                        SP
                      </div>
                    )}
                    <div className="min-w-0">
                      <p className="font-bold text-slate-950 truncate">{item.productName}</p>
                      {item.variantName ? (
                        <p className="text-xs text-slate-500">{item.variantName}</p>
                      ) : null}
                      <p className="text-xs text-slate-600 mt-0.5">
                        {money(item.unitPriceVnd)} × {item.quantity} = <span className="font-semibold">{money(item.lineTotalVnd)}</span>
                      </p>
                    </div>
                  </div>

                  {order.status === "COMPLETED" ? (
                    <Button
                      className="gap-1.5"
                      size="sm"
                      variant="outline"
                      onClick={() => setReviewItem(item)}
                    >
                      <Star className="size-3.5 fill-amber-400 text-amber-500" />
                      Đánh giá sản phẩm
                    </Button>
                  ) : null}
                </div>
              ))}
            </div>
          </div>
        ) : null}

        {order.availableActions.includes("CONFIRM_RECEIVED") ? (
          <div className="mt-8 rounded-2xl border border-emerald-200 bg-emerald-50 p-5">
            <div className="flex gap-3">
              <PackageCheck className="mt-0.5 size-6 text-emerald-700" />
              <div>
                <h2 className="font-bold text-emerald-950">Bạn đã nhận được hàng?</h2>
                <p className="mt-1 text-sm text-emerald-800">Với đơn COD, thao tác này đồng thời xác nhận đã thanh toán và hoàn tất đơn.</p>
              </div>
            </div>
            <Button className="mt-4" disabled={submitting} onClick={() => void confirmReceived()}>
              {submitting ? "Đang xác nhận…" : "Đã nhận hàng"}
            </Button>
          </div>
        ) : null}

        {order.status === "COMPLETED" ? (
          <div className="mt-8 flex items-center justify-between gap-3 rounded-2xl bg-emerald-50 p-5 text-emerald-900 border border-emerald-200">
            <div className="flex items-center gap-3">
              <CheckCircle2 className="size-6 text-emerald-600 shrink-0" />
              <div>
                <p className="font-bold">Đơn hàng đã hoàn tất thành công.</p>
                <p className="text-xs text-emerald-800">Cảm ơn bạn đã mua sắm! Hãy chia sẻ đánh giá về các sản phẩm đã mua.</p>
              </div>
            </div>
          </div>
        ) : null}

        {error ? <p className="mt-4 text-sm text-danger">{error}</p> : null}
      </SurfacePanel>

      <CreateReviewModal
        isOpen={!!reviewItem}
        orderItemId={reviewItem?.itemId}
        productName={reviewItem?.productName}
        onClose={() => setReviewItem(null)}
      />
    </main>
  );
}
