"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { CheckCircle2, CircleAlert, Clock3, RefreshCw, XCircle } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { paymentMethodLabel } from "@/features/order/utils/order-format";
import { paymentReturnApi, type PaymentReturnStatus } from "../api/payment-return.api";

export function PaymentResultPanel() {
  const searchParams = useSearchParams();
  const reference = searchParams.get("vnp_TxnRef")
    ?? searchParams.get("apptransid")
    ?? searchParams.get("app_trans_id")
    ?? searchParams.get("orderCode")
    ?? searchParams.get("reference");
  const [payment, setPayment] = useState<PaymentReturnStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [pollCount, setPollCount] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async (showLoading = true) => {
    if (!reference) {
      setError("Cổng thanh toán không trả về mã tham chiếu hợp lệ.");
      setLoading(false);
      return;
    }
    if (showLoading) setLoading(true);
    setError(null);
    try {
      setPayment(await paymentReturnApi.status(reference));
    } catch {
      setError("Chưa thể đọc trạng thái thanh toán. Hãy đăng nhập lại hoặc thử lại sau vài giây.");
    } finally {
      setLoading(false);
    }
  }, [reference]);

  useEffect(() => {
    const timer = window.setTimeout(() => void refresh(), 0);
    return () => window.clearTimeout(timer);
  }, [refresh]);

  useEffect(() => {
    if (!payment || payment.status !== "PENDING" || pollCount >= 20) return;
    const timer = window.setTimeout(() => {
      setPollCount((value) => value + 1);
      void refresh(false);
    }, 3000);
    return () => window.clearTimeout(timer);
  }, [payment, pollCount, refresh]);

  const failed = payment?.status === "FAILED" || payment?.status === "EXPIRED";
  const succeeded = payment?.status === "PAID";
  const requiresManualReview = failed || (payment?.status === "PENDING" && pollCount >= 20);
  const Icon = succeeded ? CheckCircle2 : failed ? XCircle : Clock3;
  const title = succeeded
    ? "Thanh toán đã được xác nhận"
    : failed
      ? "Thanh toán chưa hoàn tất"
      : "Đang xác minh thanh toán";
  const detail = succeeded
    ? `Hệ thống đã nhận thông báo hợp lệ từ ${paymentMethodLabel(payment?.method ?? "cổng thanh toán")}.`
    : failed
      ? "Cổng thanh toán chưa xác nhận được giao dịch này. Nếu ngân hàng báo đã trừ tiền, đừng thanh toán lại."
      : pollCount >= 20
        ? "Chưa nhận được xác nhận từ cổng thanh toán. Đừng tạo hoặc thanh toán một đơn khác cho đến khi đã kiểm tra trạng thái."
        : "Trang quay lại không phải bằng chứng thanh toán. Hệ thống đang chờ thông báo có chữ ký và số tiền hợp lệ.";

  return (
    <main className="mx-auto flex min-h-[60vh] max-w-xl items-center px-4 py-12 text-center">
      <section className="w-full rounded-3xl border border-border bg-surface p-8 shadow-sm">
        <Icon className={`mx-auto size-12 ${succeeded ? "text-emerald-600" : failed || error ? "text-danger" : "text-amber-500"}`} />
        <h1 className="mt-4 text-2xl font-bold">
          {error ? "Chưa đọc được trạng thái" : loading ? "Đang kiểm tra với máy chủ" : title}
        </h1>
        <p className="mt-3 text-sm leading-6 text-muted-foreground">
          {error ?? (loading ? "Vui lòng chờ trong giây lát." : detail)}
        </p>
        {payment ? <div className="mt-3 space-y-1 font-mono text-xs text-slate-400"><p>Mã đơn: {payment.orderId}</p><p>Mã tham chiếu: {reference}</p></div> : null}
        {requiresManualReview ? <div className="mt-5 flex gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-left text-sm text-amber-950"><CircleAlert className="mt-0.5 size-5 shrink-0 text-amber-700" /><div><p className="font-black">Nếu tài khoản đã bị trừ tiền</p><p className="mt-1 leading-5">Giữ lại mã đơn và mã tham chiếu ở trên, không thanh toán lần hai. Bộ phận hỗ trợ sẽ đối soát với cổng thanh toán và xử lý hoàn tiền thủ công nếu giao dịch đã được ghi nhận.</p></div></div> : null}
        <div className="mt-6 flex flex-wrap justify-center gap-3">
          {payment ? (
            <Link className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-slate-50" href={`/customer/account/orders/${payment.orderId}`}>
              Xem đơn hàng
            </Link>
          ) : (
            <Link className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-slate-50" href="/">
              Về trang chủ
            </Link>
          )}
          {requiresManualReview ? <Link className="rounded-lg border border-amber-300 bg-amber-50 px-4 py-2 text-sm font-bold text-amber-900 hover:bg-amber-100" href={`/contact?subject=payment&reference=${encodeURIComponent(reference ?? "")}`}>Liên hệ hỗ trợ</Link> : null}
          {!succeeded ? <Button onClick={() => void refresh(true)}><RefreshCw />Kiểm tra lại</Button> : null}
        </div>
      </section>
    </main>
  );
}
