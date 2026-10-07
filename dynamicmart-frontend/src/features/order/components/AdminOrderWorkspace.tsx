"use client";

import { Search, ShieldAlert } from "lucide-react";
import { FormEvent, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { ErrorState, LoadingState } from "@/components/common/PageState";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { isApiError } from "@/lib/api/error";
import { useAuthSession } from "@/lib/auth/session";
import { adminOrdersApi } from "../api/admin-orders.api";
import type { OrderAction, OrderDetail, OrderPage, OrderStatus } from "../types/order.types";
import { formatDateTime, formatVnd, orderActorLabel, orderStatusLabel, orderStatusTone, paymentMethodLabel, paymentTimingLabel } from "../utils/order-format";

const statuses: OrderStatus[] = ["PENDING_PAYMENT", "CONFIRMED", "PACKING", "SHIPPING", "HANDOVER_PENDING", "DELIVERED", "COMPLETED", "CANCELLED"];
const actionLabel: Record<Exclude<OrderAction, "CONFIRM_RECEIVED">, string> = { PACK: "Bắt đầu đóng gói", SHIP: "Chuyển sang đang giao", HANDOVER: "Chuyển sang chờ bàn giao" };

export function AdminOrderWorkspace() {
  const router = useRouter();
  const session = useAuthSession();
  const [data, setData] = useState<OrderPage | null>(null);
  const [selected, setSelected] = useState<OrderDetail | null>(null);
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<OrderStatus | "">("");
  const [orderNumber, setOrderNumber] = useState("");
  const [customerId, setCustomerId] = useState("");
  const [applied, setApplied] = useState({ orderNumber: "", customerId: "" });
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");
  const operationKeys = useRef(new Map<string, string>());
  const isAdmin = session.status === "authenticated" && session.user.role === "ADMIN";

  useEffect(() => {
    if (session.status === "anonymous") {
      router.replace(`/login?returnTo=${encodeURIComponent("/admin/orders")}`);
    }
  }, [router, session.status]);

  async function loadList() {
    setLoading(true);
    setError("");
    try {
      setData(await adminOrdersApi.list({ page, size: 20, status, orderNumber: applied.orderNumber, customerId: applied.customerId }));
    } catch (cause) {
      setError(isApiError(cause) ? cause.message : "Không thể tải danh sách đơn hàng.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!isAdmin) return;
    let ignored = false;
    adminOrdersApi.list({ page, size: 20, status, orderNumber: applied.orderNumber, customerId: applied.customerId })
      .then((result) => { if (!ignored) { setData(result); setError(""); } })
      .catch((cause) => { if (!ignored) setError(isApiError(cause) ? cause.message : "Không thể tải danh sách đơn hàng."); })
      .finally(() => { if (!ignored) setLoading(false); });
    return () => { ignored = true; };
  }, [applied, isAdmin, page, status]);

  async function inspect(orderId: string) {
    setDetailLoading(true);
    setError("");
    try {
      const [detail, timeline] = await Promise.all([adminOrdersApi.get(orderId), adminOrdersApi.timeline(orderId)]);
      setSelected({ ...detail, timeline: timeline.timeline });
    } catch (cause) {
      setError(isApiError(cause) ? cause.message : "Không thể tải chi tiết đơn hàng.");
    } finally {
      setDetailLoading(false);
    }
  }

  async function execute(action: Exclude<OrderAction, "CONFIRM_RECEIVED">) {
    if (!selected || working) return;
    if (!window.confirm(`${actionLabel[action]} cho đơn ${selected.orderNumber}?`)) return;
    const operation = `${selected.orderId}:${action}`;
    const key = operationKeys.current.get(operation) ?? crypto.randomUUID();
    operationKeys.current.set(operation, key);
    setWorking(true);
    setError("");
    try {
      if (action === "PACK") await adminOrdersApi.pack(selected.orderId, key);
      if (action === "SHIP") await adminOrdersApi.ship(selected.orderId, key);
      if (action === "HANDOVER") await adminOrdersApi.handover(selected.orderId, key);
      operationKeys.current.delete(operation);
      await Promise.all([inspect(selected.orderId), loadList()]);
    } catch (cause) {
      setError(isApiError(cause) ? cause.message : "Không thể cập nhật trạng thái đơn hàng.");
    } finally {
      setWorking(false);
    }
  }

  function search(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setPage(0);
    setApplied({ orderNumber: orderNumber.trim(), customerId: customerId.trim() });
  }

  if (session.status === "loading" || session.status === "anonymous") return <LoadingState title="Đang kiểm tra quyền quản trị" />;
  if (!isAdmin) {
    return <ErrorState actionLabel="Về trang chủ" description="Chỉ tài khoản quản trị viên được mở khu vực quản lý đơn hàng." onAction={() => router.push("/")} title="Không có quyền truy cập" />;
  }

  return <div className="space-y-7"><PageHeader eyebrow="Vận hành bán hàng" title="Quản lý đơn hàng" description="Theo dõi thông tin đã chốt, lịch sử xử lý và chuyển đơn qua từng giai đoạn hợp lệ." />
    <SurfacePanel><form className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_220px_auto] lg:items-end" onSubmit={search}><Input label="Mã đơn" value={orderNumber} onChange={(event) => setOrderNumber(event.target.value)} /><Input label="Mã khách hàng" value={customerId} onChange={(event) => setCustomerId(event.target.value)} /><Select label="Trạng thái" value={status} onChange={(event) => { setLoading(true); setError(""); setStatus(event.target.value as OrderStatus | ""); setPage(0); }}><option value="">Tất cả trạng thái</option>{statuses.map((value) => <option key={value} value={value}>{orderStatusLabel(value)}</option>)}</Select><Button type="submit"><Search />Lọc</Button></form></SurfacePanel>
    {error ? <div className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"><ShieldAlert className="mt-0.5 size-5 shrink-0" />{error}</div> : null}
    {loading ? <LoadingState title="Đang tải đơn hàng" /> : <SurfacePanel className="overflow-hidden p-0"><div className="overflow-x-auto"><table className="min-w-full text-left text-sm"><thead className="bg-stone-50 text-xs font-bold uppercase text-stone-500"><tr><th className="px-5 py-4">Đơn hàng</th><th className="px-5 py-4">Khách hàng</th><th className="px-5 py-4">Thanh toán</th><th className="px-5 py-4">Tổng tiền</th><th className="px-5 py-4">Trạng thái</th><th className="px-5 py-4" /></tr></thead><tbody>{data?.content.length ? data.content.map((order) => <tr className="border-t border-stone-100" key={order.orderId}><td className="px-5 py-4"><strong>{order.orderNumber}</strong><p className="mt-1 text-xs text-stone-500">{formatDateTime(order.createdAt)}</p></td><td className="px-5 py-4 font-mono text-xs">{order.customerId}</td><td className="px-5 py-4"><span className="block font-semibold">{paymentMethodLabel(order.paymentMethod)}</span><span className="mt-1 block text-xs text-stone-500">{paymentTimingLabel(order.paymentTiming)}</span></td><td className="px-5 py-4 font-black">{formatVnd(order.finalTotalVnd)}</td><td className="px-5 py-4"><StatusBadge label={orderStatusLabel(order.status)} tone={orderStatusTone(order.status)} /></td><td className="px-5 py-4"><Button variant="outline" onClick={() => void inspect(order.orderId)}>Chi tiết</Button></td></tr>) : <tr><td className="px-5 py-12 text-center text-stone-500" colSpan={6}>Không tìm thấy đơn hàng phù hợp.</td></tr>}</tbody></table></div><div className="flex items-center justify-end gap-3 border-t px-5 py-4"><Button variant="outline" disabled={!data || data.first} onClick={() => { setLoading(true); setPage((value) => Math.max(0, value - 1)); }}>Trang trước</Button><span className="text-sm text-stone-500">{(data?.page ?? 0) + 1}/{Math.max(data?.totalPages ?? 1, 1)}</span><Button variant="outline" disabled={!data || data.last} onClick={() => { setLoading(true); setPage((value) => value + 1); }}>Trang sau</Button></div></SurfacePanel>}
    {detailLoading ? <LoadingState title="Đang tải chi tiết đơn hàng" /> : selected ? <AdminOrderDetail order={selected} working={working} close={() => setSelected(null)} execute={execute} /> : null}
  </div>;
}

function AdminOrderDetail({ order, working, close, execute }: Readonly<{ order: OrderDetail; working: boolean; close: () => void; execute: (action: Exclude<OrderAction, "CONFIRM_RECEIVED">) => Promise<void> }>) {
  const actions = order.availableActions.filter((value): value is Exclude<OrderAction, "CONFIRM_RECEIVED"> => value !== "CONFIRM_RECEIVED");
  return <SurfacePanel><div className="flex flex-wrap items-start justify-between gap-4"><div><p className="text-xs font-black uppercase tracking-wider text-brand">Chi tiết đơn hàng</p><h2 className="mt-2 text-xl font-black">{order.orderNumber}</h2><p className="mt-1 font-mono text-xs text-stone-500">{order.orderId}</p></div><div className="flex flex-wrap items-center gap-2"><StatusBadge label={orderStatusLabel(order.status)} tone={orderStatusTone(order.status)} />{actions.map((action) => <Button disabled={working} key={action} onClick={() => void execute(action)}>{working ? "Đang xử lý…" : actionLabel[action]}</Button>)}<Button variant="outline" onClick={close}>Đóng</Button></div></div><div className="mt-6 grid gap-6 lg:grid-cols-3"><section><h3 className="font-bold">Khách hàng và giao hàng</h3><dl className="mt-3 grid grid-cols-2 gap-3 text-sm"><dt className="text-stone-500">Mã khách hàng</dt><dd className="break-all font-mono text-xs">{order.customerId}</dd><dt className="text-stone-500">Người nhận</dt><dd>{order.address.recipientName}</dd><dt className="text-stone-500">Điện thoại</dt><dd>{order.address.phone}</dd><dt className="text-stone-500">Địa chỉ</dt><dd>{order.address.addressLine}, {order.address.wardName}, {order.address.provinceName}</dd></dl></section><section><h3 className="font-bold">Tiền và thanh toán</h3><dl className="mt-3 grid grid-cols-2 gap-3 text-sm"><dt className="text-stone-500">Tổng tiền</dt><dd className="font-black">{formatVnd(order.money.finalTotalVnd)}</dd><dt className="text-stone-500">Phương thức</dt><dd>{paymentMethodLabel(order.paymentMethod)} · {paymentTimingLabel(order.paymentTiming)}</dd><dt className="text-stone-500">Đã thanh toán</dt><dd>{formatDateTime(order.paymentSucceededAt)}</dd><dt className="text-stone-500">Phí giao hàng</dt><dd>{formatVnd(order.shipping.payableFeeVnd)}</dd></dl></section><section><h3 className="font-bold">Lịch sử xử lý</h3><ol className="mt-3 max-h-72 space-y-3 overflow-y-auto">{order.timeline.map((entry) => <li className="border-l-2 border-rose-200 pl-3 text-sm" key={entry.historyId}><strong>{orderStatusLabel(entry.toStatus)}</strong><p className="text-xs text-stone-500">{formatDateTime(entry.createdAt)} · {orderActorLabel(entry.actorType)}</p>{entry.reason ? <p className="mt-1 text-xs text-stone-600">{entry.reason}</p> : null}</li>)}</ol></section></div><section className="mt-6 border-t border-stone-200 pt-5"><h3 className="font-bold">Sản phẩm trong đơn</h3><div className="mt-3 overflow-x-auto rounded-xl border border-stone-200"><table className="min-w-full text-left text-sm"><thead className="bg-stone-50 text-xs uppercase text-stone-500"><tr><th className="px-4 py-3">Sản phẩm</th><th className="px-4 py-3">Mã hàng</th><th className="px-4 py-3">Số lượng</th><th className="px-4 py-3">Đơn giá</th><th className="px-4 py-3">Thành tiền</th></tr></thead><tbody>{order.items.map((item) => <tr className="border-t border-stone-100" key={item.itemId}><td className="px-4 py-3"><strong>{item.productName}</strong><p className="text-xs text-stone-500">{item.variantName}</p></td><td className="px-4 py-3 font-mono text-xs">{item.sku}</td><td className="px-4 py-3">{item.quantity}</td><td className="px-4 py-3">{formatVnd(item.unitPriceVnd)}</td><td className="px-4 py-3 font-bold">{formatVnd(item.lineTotalVnd)}</td></tr>)}</tbody></table></div></section></SurfacePanel>;
}
