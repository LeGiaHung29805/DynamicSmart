"use client";

import { ArrowLeft } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ErrorState, LoadingState } from "@/components/common/PageState";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/Button";
import { customerApi } from "@/features/customer/api/customer.api";
import type { AdminUser } from "@/features/customer/types/customer.types";
import { adminOrdersApi } from "@/features/order/api/admin-orders.api";
import type { OrderAction, OrderDetail } from "@/features/order/types/order.types";
import {
  formatDateTime,
  formatVnd,
  orderStatusLabel,
  orderStatusTone,
  paymentMethodLabel,
  paymentTimingLabel,
} from "@/features/order/utils/order-format";
import {
  adminPaymentsApi,
  type AdminPayment,
  type CallbackAudit,
  type PaymentAttempt,
} from "@/features/payment/api/admin-payments.api";
import {
  callbackResultLabel,
  paymentAttemptStatusLabel,
  paymentStatusLabel,
  paymentStatusTone,
} from "@/features/payment/utils/payment-format";

function BackButton({ href }: { href: string }) {
  const router = useRouter();
  return (
    <Button variant="secondary" onClick={() => router.push(href)}>
      <ArrowLeft className="h-4 w-4" /> Quay lại
    </Button>
  );
}

function DetailField({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="rounded-xl border border-slate-100 bg-slate-50/70 p-3">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <div className="mt-1 break-words text-sm font-semibold text-slate-900">{value || "—"}</div>
    </div>
  );
}

const userStatusLabel: Record<AdminUser["status"], string> = {
  ACTIVE: "Đang hoạt động",
  LOCKED: "Đã khóa",
  DISABLED: "Đã vô hiệu hóa",
};

const userStatusTone: Record<AdminUser["status"], "success" | "warning" | "danger"> = {
  ACTIVE: "success",
  LOCKED: "warning",
  DISABLED: "danger",
};

export function AdminUserDetailPage({ userId }: { userId: string }) {
  const [user, setUser] = useState<AdminUser | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setUser(await customerApi.user(userId));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tải thông tin tài khoản.");
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const updateUser = async (patch: { role?: AdminUser["role"]; status?: AdminUser["status"] }, action: string) => {
    if (!user || saving) return;
    const reason = window.prompt(`Lý do ${action.toLowerCase()}:`)?.trim();
    if (!reason) return;
    setSaving(true);
    setError("");
    try {
      await customerApi.manageUser(user.id, { ...patch, reason });
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : `Không thể ${action.toLowerCase()}.`);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingState title="Đang tải tài khoản..." />;
  if (!user) return <ErrorState description={error || "Không tìm thấy tài khoản."} onAction={() => void load()} />;

  const isCustomer = user.role === "CUSTOMER";
  return (
    <div className="space-y-6">
      <PageHeader title="Chi tiết tài khoản" description="Xem lịch sử và quản lý quyền, trạng thái tài khoản." action={<BackButton href="/admin/users" />} />
      {error ? <ErrorState description={error} onAction={() => void load()} /> : null}
      <SurfacePanel className="space-y-5 p-5 sm:p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-lg font-bold text-slate-950">{user.fullName}</h2>
            <p className="mt-1 text-sm text-slate-500">{user.email}</p>
          </div>
          <StatusBadge tone={userStatusTone[user.status]} label={userStatusLabel[user.status]} />
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <DetailField label="Vai trò" value={isCustomer ? "Khách hàng" : "Quản trị viên"} />
          <DetailField label="Điện thoại" value={user.phone} />
          <DetailField label="Ngày tạo" value={formatDateTime(user.createdAt)} />
          <DetailField label="Đăng nhập gần nhất" value={formatDateTime(user.lastLoginAt)} />
        </div>
        <div className="border-t border-slate-100 pt-5">
          <h3 className="text-sm font-bold text-slate-900">Cập nhật tài khoản</h3>
          <p className="mt-1 text-sm text-slate-500">Mỗi thay đổi yêu cầu nhập lý do để lưu vào lịch sử quản trị.</p>
          <div className="mt-3 flex flex-wrap gap-2">
            <Button disabled={saving} variant="secondary" onClick={() => void updateUser({ role: isCustomer ? "ADMIN" : "CUSTOMER" }, isCustomer ? "cấp quyền quản trị" : "gỡ quyền quản trị")}>
              {isCustomer ? "Cấp quyền quản trị" : "Gỡ quyền quản trị"}
            </Button>
            {user.status === "ACTIVE" ? (
              <Button disabled={saving} variant="secondary" onClick={() => void updateUser({ status: "LOCKED" }, "khóa tài khoản")}>Khóa tài khoản</Button>
            ) : user.status === "LOCKED" ? (
              <Button disabled={saving} variant="secondary" onClick={() => void updateUser({ status: "ACTIVE" }, "mở khóa tài khoản")}>Mở khóa tài khoản</Button>
            ) : null}
            {user.status !== "DISABLED" ? (
              <Button disabled={saving} variant="destructive" onClick={() => void updateUser({ status: "DISABLED" }, "vô hiệu hóa tài khoản")}>Vô hiệu hóa</Button>
            ) : null}
          </div>
        </div>
      </SurfacePanel>
      <SurfacePanel className="p-5 sm:p-6">
        <h2 className="text-base font-bold text-slate-950">Lịch sử quản trị</h2>
        <div className="mt-4 space-y-3">
          {user.audits?.length ? user.audits.map((audit) => (
            <div key={audit.id} className="rounded-xl border border-slate-100 p-3 text-sm">
              <p className="font-semibold text-slate-900">{audit.action}</p>
              <p className="mt-1 text-slate-600">{audit.reason || "Không có ghi chú."}</p>
              <p className="mt-1 text-xs text-slate-500">{formatDateTime(audit.createdAt)}</p>
            </div>
          )) : <p className="text-sm text-slate-500">Chưa có lịch sử quản trị.</p>}
        </div>
      </SurfacePanel>
    </div>
  );
}

const orderActionLabel: Partial<Record<OrderAction, string>> = {
  PACK: "Bắt đầu đóng gói",
  SHIP: "Chuyển sang đang giao",
  HANDOVER: "Chuyển sang chờ khách xác nhận",
};

export function AdminOrderDetailPage({ orderId }: { orderId: string }) {
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [savingAction, setSavingAction] = useState<OrderAction | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [detail, timeline] = await Promise.all([adminOrdersApi.get(orderId), adminOrdersApi.timeline(orderId)]);
      setOrder({ ...detail, timeline: timeline.timeline });
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

  const runAction = async (action: OrderAction) => {
    if (!order || savingAction || !orderActionLabel[action]) return;
    if (!window.confirm(`Xác nhận: ${orderActionLabel[action]}?`)) return;
    setSavingAction(action);
    setError("");
    try {
      const key = crypto.randomUUID();
      if (action === "PACK") await adminOrdersApi.pack(order.orderId, key);
      if (action === "SHIP") await adminOrdersApi.ship(order.orderId, key);
      if (action === "HANDOVER") await adminOrdersApi.handover(order.orderId, key);
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể cập nhật trạng thái đơn hàng.");
    } finally {
      setSavingAction(null);
    }
  };

  if (loading) return <LoadingState title="Đang tải đơn hàng..." />;
  if (!order) return <ErrorState description={error || "Không tìm thấy đơn hàng."} onAction={() => void load()} />;

  const actions = (order.availableActions || []).filter((action) => orderActionLabel[action]);
  return (
    <div className="space-y-6">
      <PageHeader title={`Đơn ${order.orderNumber}`} description="Theo dõi đơn hàng và cập nhật đúng bước vận hành." action={<BackButton href="/admin/orders" />} />
      {error ? <ErrorState description={error} onAction={() => void load()} /> : null}
      <SurfacePanel className="space-y-5 p-5 sm:p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div><p className="text-sm text-slate-500">Trạng thái đơn hàng</p><h2 className="mt-1 text-lg font-bold text-slate-950">{orderStatusLabel(order.status)}</h2></div>
          <StatusBadge tone={orderStatusTone(order.status)} label={orderStatusLabel(order.status)} />
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <DetailField label="Tổng thanh toán" value={formatVnd(order.money.finalTotalVnd)} />
          <DetailField label="Phí vận chuyển" value={formatVnd(order.shipping.payableFeeVnd)} />
          <DetailField label="Thanh toán" value={`${paymentMethodLabel(order.paymentMethod)} · ${paymentTimingLabel(order.paymentTiming)}`} />
        </div>
        <div className="border-t border-slate-100 pt-5">
          <h3 className="text-sm font-bold text-slate-900">Cập nhật trạng thái</h3>
          <p className="mt-1 text-sm text-slate-500">Admin điều phối đến bước chờ khách xác nhận. Chỉ khách hoặc hệ thống sau 7 ngày mới có thể xác nhận đã nhận hàng.</p>
          <div className="mt-3 flex flex-wrap gap-2">
            {actions.length ? actions.map((action) => (
              <Button key={action} disabled={savingAction !== null} onClick={() => void runAction(action)}>
                {savingAction === action ? "Đang cập nhật..." : orderActionLabel[action]}
              </Button>
            )) : <p className="text-sm text-slate-500">{order.status === "CANCELLED" || order.status === "COMPLETED" ? "Đơn đã ở trạng thái kết thúc nên không thể chuyển tiếp." : "Chưa có thao tác trạng thái nào khả dụng cho đơn này."}</p>}
          </div>
        </div>
      </SurfacePanel>
      <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
        <SurfacePanel className="p-5 sm:p-6"><h2 className="text-base font-bold text-slate-950">Sản phẩm</h2><div className="mt-4 space-y-3">{order.items.map((item) => <div key={item.itemId} className="flex items-center justify-between gap-4 border-b border-slate-100 pb-3 text-sm last:border-0 last:pb-0"><div className="flex min-w-0 items-center gap-3">{item.imageUrl ? <img alt={item.productName} className="h-14 w-14 shrink-0 rounded-lg border border-slate-100 object-cover" onError={(event) => { event.currentTarget.style.display = "none"; }} src={item.imageUrl} /> : <div aria-label="Chưa có ảnh sản phẩm" className="flex h-14 w-14 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-xs font-bold text-slate-500">DM</div>}<div className="min-w-0"><p className="truncate font-semibold text-slate-900">{item.productName}</p><p className="truncate text-slate-500">{item.variantName || item.sku} · SL {item.quantity}</p><p className="text-xs text-slate-400">SKU: {item.sku}</p></div></div><p className="font-semibold whitespace-nowrap">{formatVnd(item.lineTotalVnd)}</p></div>)}</div></SurfacePanel>
        <SurfacePanel className="p-5 sm:p-6"><h2 className="text-base font-bold text-slate-950">Giao hàng</h2><div className="mt-4 space-y-2 text-sm text-slate-700"><p className="font-semibold text-slate-950">{order.address.recipientName} · {order.address.phone}</p><p>{[order.address.addressLine, order.address.wardName, order.address.provinceName].filter(Boolean).join(", ")}</p></div></SurfacePanel>
      </div>
      <SurfacePanel className="p-5 sm:p-6"><h2 className="text-base font-bold text-slate-950">Lịch sử đơn hàng</h2><div className="mt-4 space-y-3">{order.timeline?.length ? order.timeline.map((item) => <div key={item.historyId} className="rounded-xl border border-slate-100 p-3"><p className="text-sm font-semibold text-slate-900">{orderStatusLabel(item.toStatus)}</p><p className="mt-1 text-xs text-slate-500">{formatDateTime(item.createdAt)}</p></div>) : <p className="text-sm text-slate-500">Chưa có lịch sử trạng thái.</p>}</div></SurfacePanel>
    </div>
  );
}

export function AdminPaymentDetailPage({ paymentId }: { paymentId: string }) {
  const [payment, setPayment] = useState<AdminPayment | null>(null);
  const [attempts, setAttempts] = useState<PaymentAttempt[]>([]);
  const [audits, setAudits] = useState<CallbackAudit[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const [detail, paymentAttempts, callbackAudits] = await Promise.all([adminPaymentsApi.get(paymentId), adminPaymentsApi.attempts(paymentId), adminPaymentsApi.audits(paymentId)]);
      setPayment(detail); setAttempts(paymentAttempts); setAudits(callbackAudits);
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Không thể tải giao dịch."); }
    finally { setLoading(false); }
  }, [paymentId]);
  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);
  if (loading) return <LoadingState title="Đang tải giao dịch..." />;
  if (!payment) return <ErrorState description={error || "Không tìm thấy giao dịch."} onAction={() => void load()} />;
  return <div className="space-y-6"><PageHeader title="Chi tiết giao dịch" description="Trạng thái thanh toán được xác thực từ cổng thanh toán, không cập nhật thủ công." action={<BackButton href="/admin/payments" />} />{error ? <ErrorState description={error} onAction={() => void load()} /> : null}<SurfacePanel className="p-5 sm:p-6"><div className="flex flex-wrap justify-between gap-3"><div><p className="text-sm text-slate-500">Mã giao dịch</p><h2 className="mt-1 font-bold text-slate-950">{payment.id}</h2></div><StatusBadge tone={paymentStatusTone(payment.status)} label={paymentStatusLabel(payment.status)} /></div><div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3"><DetailField label="Số tiền" value={formatVnd(payment.amountVnd)} /><DetailField label="Phương thức" value={paymentMethodLabel(payment.method)} /><DetailField label="Khởi tạo" value={formatDateTime(payment.paidAt || payment.expiresAt)} /></div></SurfacePanel><div className="grid gap-6 xl:grid-cols-2"><SurfacePanel className="p-5 sm:p-6"><h2 className="font-bold text-slate-950">Lần thanh toán</h2><div className="mt-4 space-y-3">{attempts.length ? attempts.map((item) => <div key={item.id} className="rounded-xl border border-slate-100 p-3"><p className="text-sm font-semibold">{paymentAttemptStatusLabel(item.status)}</p><p className="mt-1 text-xs text-slate-500">{formatDateTime(item.createdAt)}</p></div>) : <p className="text-sm text-slate-500">Chưa có dữ liệu.</p>}</div></SurfacePanel><SurfacePanel className="p-5 sm:p-6"><h2 className="font-bold text-slate-950">Nhật ký callback</h2><div className="mt-4 space-y-3">{audits.length ? audits.map((item) => <div key={item.id} className="rounded-xl border border-slate-100 p-3"><p className="text-sm font-semibold">{callbackResultLabel(item.processedResult)}</p><p className="mt-1 text-xs text-slate-500">{formatDateTime(item.receivedAt)}</p></div>) : <p className="text-sm text-slate-500">Chưa có callback.</p>}</div></SurfacePanel></div></div>;
}
