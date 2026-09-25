"use client";

import Link from "next/link";
import { CheckCircle2, Clock3, RefreshCw, XCircle } from "lucide-react";
import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { paymentReturnApi, type PaymentReturnStatus } from "../api/payment-return.api";

export function PaymentResultPanel() {
  const searchParams = useSearchParams();
  const reference = searchParams.get("vnp_TxnRef");
  const [payment, setPayment] = useState<PaymentReturnStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!reference) { setError("VNPay không trả về mã tham chiếu hợp lệ."); setLoading(false); return; }
    setLoading(true); setError(null);
    try { setPayment(await paymentReturnApi.status(reference)); }
    catch { setError("Chưa thể đọc trạng thái thanh toán. Hãy đăng nhập lại hoặc thử lại sau vài giây."); }
    finally { setLoading(false); }
  }, [reference]);
  useEffect(() => {
    const timer = window.setTimeout(() => void refresh(), 0);
    return () => window.clearTimeout(timer);
  }, [refresh]);

  const failed = payment?.status === "FAILED" || payment?.status === "EXPIRED";
  const succeeded = payment?.status === "PAID";
  const Icon = succeeded ? CheckCircle2 : failed ? XCircle : Clock3;
  const title = succeeded ? "Thanh toán đã được xác nhận" : failed ? "Thanh toán chưa hoàn tất" : "Đang xác minh thanh toán";
  const detail = succeeded ? "Payment Service đã nhận IPN hợp lệ từ VNPay." : failed ? "Khoản trả trước đã thất bại hoặc hết hạn." : "Return URL không phải bằng chứng thanh toán. Hệ thống đang chờ IPN có chữ ký và số tiền hợp lệ.";

  return <main className="mx-auto flex min-h-[60vh] max-w-xl items-center px-4 py-12 text-center"><section className="w-full rounded-xl border border-border bg-surface p-8"><Icon className={`mx-auto size-12 ${succeeded ? "text-emerald-600" : failed || error ? "text-danger" : "text-amber-500"}`} /><h1 className="mt-4 text-2xl font-bold">{error ? "Chưa đọc được trạng thái" : loading ? "Đang kiểm tra với máy chủ" : title}</h1><p className="mt-3 text-sm leading-6 text-muted">{error ?? (loading ? "Vui lòng chờ trong giây lát." : detail)}</p>{payment ? <p className="mt-3 font-mono text-xs text-slate-400">Order: {payment.orderId}</p> : null}<div className="mt-6 flex flex-wrap justify-center gap-3"><Link className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-slate-50" href="/">Về trang chủ</Link>{!succeeded ? <Button onClick={() => void refresh()}><RefreshCw /> Kiểm tra lại</Button> : null}</div></section></main>;
}
