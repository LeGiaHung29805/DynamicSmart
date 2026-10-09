"use client";

import Link from "next/link";
import { PackageSearch, Search } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { EmptyState, ErrorState, LoadingState } from "@/components/common/PageState";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { isApiError } from "@/lib/api/error";
import { useAuthSession } from "@/lib/auth/session";
import { customerOrdersApi } from "../api/customer-orders.api";
import type { OrderPage, OrderStatus } from "../types/order.types";
import { formatDateTime, formatVnd, orderStatusLabel, orderStatusTone, paymentMethodLabel, paymentTimingLabel } from "../utils/order-format";

const statuses: OrderStatus[] = ["PENDING_PAYMENT", "CONFIRMED", "PACKING", "SHIPPING", "HANDOVER_PENDING", "DELIVERED", "COMPLETED", "CANCELLED"];

export function CustomerOrderList() {
  const router = useRouter();
  const session = useAuthSession();
  const [data, setData] = useState<OrderPage | null>(null);
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<OrderStatus | "">("");
  const [orderNumber, setOrderNumber] = useState("");
  const [appliedOrderNumber, setAppliedOrderNumber] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reloadNonce, setReloadNonce] = useState(0);

  useEffect(() => {
    if (session.status === "loading") return;
    if (session.status === "anonymous") {
      router.replace("/login?returnTo=%2Fcustomer%2Faccount%2Forders");
      return;
    }
    let ignored = false;
    customerOrdersApi.list({ page, size: 10, status, orderNumber: appliedOrderNumber, sort: "createdAt,desc" })
      .then((result) => { if (!ignored) { setData(result); setError(""); } })
      .catch((cause) => { if (!ignored) setError(isApiError(cause) ? cause.message : "Không thể tải danh sách đơn hàng."); })
      .finally(() => { if (!ignored) setLoading(false); });
    return () => { ignored = true; };
  }, [appliedOrderNumber, page, reloadNonce, router, session.status, status]);

  function search(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setPage(0);
    setAppliedOrderNumber(orderNumber.trim());
  }

  if (session.status === "anonymous") return null;
  return <div className="space-y-7"><PageHeader eyebrow="Tài khoản" title="Đơn hàng của tôi" description="Theo dõi trạng thái, thanh toán, giao hàng và lịch sử xử lý của từng đơn." />
    <SurfacePanel><form className="grid gap-4 md:grid-cols-[minmax(0,1fr)_220px_auto] md:items-end" onSubmit={search}><Input label="Mã đơn hàng" placeholder="Ví dụ: DM-2026..." value={orderNumber} onChange={(event) => setOrderNumber(event.target.value)} /><Select label="Trạng thái" value={status} onChange={(event) => { setLoading(true); setError(""); setStatus(event.target.value as OrderStatus | ""); setPage(0); }}><option value="">Tất cả trạng thái</option>{statuses.map((value) => <option key={value} value={value}>{orderStatusLabel(value)}</option>)}</Select><Button type="submit"><Search />Tìm đơn</Button></form></SurfacePanel>
    {loading || session.status === "loading" ? <LoadingState title="Đang tải đơn hàng" /> : error ? <ErrorState description={error} onAction={() => { setLoading(true); setError(""); setReloadNonce((value) => value + 1); }} /> : !data?.content.length ? <EmptyState title="Chưa có đơn hàng" description="Các đơn bạn tạo thành công sẽ xuất hiện tại đây." /> : <SurfacePanel className="overflow-hidden p-0"><div className="overflow-x-auto"><table className="min-w-full text-left text-sm"><thead className="bg-stone-50 text-xs font-bold tracking-wide text-stone-500 uppercase"><tr><th className="px-5 py-4">Đơn hàng</th><th className="px-5 py-4">Ngày tạo</th><th className="px-5 py-4">Thanh toán</th><th className="px-5 py-4">Tổng tiền</th><th className="px-5 py-4">Trạng thái</th><th className="px-5 py-4" /></tr></thead><tbody>{data.content.map((order) => <tr className="border-t border-stone-100" key={order.orderId}><td className="px-5 py-4"><strong className="block text-stone-900">{order.orderNumber}</strong><code className="mt-1 block text-[11px] text-stone-400">{order.orderId}</code></td><td className="px-5 py-4 text-stone-600">{formatDateTime(order.createdAt)}</td><td className="px-5 py-4 text-stone-600"><span className="block font-semibold text-stone-800">{paymentMethodLabel(order.paymentMethod)}</span><span className="mt-1 block text-xs">{paymentTimingLabel(order.paymentTiming)}</span></td><td className="px-5 py-4 font-black text-stone-900">{formatVnd(order.finalTotalVnd)}</td><td className="px-5 py-4"><StatusBadge label={orderStatusLabel(order.status)} tone={orderStatusTone(order.status)} /></td><td className="px-5 py-4 text-right"><Link className="inline-flex rounded-xl border border-stone-200 px-4 py-2 font-bold text-stone-800 hover:border-rose-300 hover:text-rose-700" href={`/customer/account/orders/${order.orderId}`}>Chi tiết</Link></td></tr>)}</tbody></table></div><div className="flex flex-wrap items-center justify-between gap-3 border-t border-stone-100 px-5 py-4"><p className="text-sm text-stone-500"><PackageSearch className="mr-2 inline size-4" />{data.totalElements} đơn hàng</p><div className="flex items-center gap-3"><Button variant="outline" disabled={data.first} onClick={() => { setLoading(true); setPage((value) => Math.max(0, value - 1)); }}>Trang trước</Button><span className="text-sm text-stone-500">{data.page + 1}/{Math.max(data.totalPages, 1)}</span><Button variant="outline" disabled={data.last} onClick={() => { setLoading(true); setPage((value) => value + 1); }}>Trang sau</Button></div></div></SurfacePanel>}
  </div>;
}
