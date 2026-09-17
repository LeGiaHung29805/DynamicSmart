import Link from "next/link";
import { CheckCircle2, Clock3, XCircle } from "lucide-react";

type PaymentResultPageProps = { searchParams: Promise<{ status?: string }> };

export const metadata = { title: "Kết quả thanh toán" };

export default async function PaymentResultPage({ searchParams }: PaymentResultPageProps) {
  const { status } = await searchParams;
  const failed = status === "failed" || status === "expired";
  const succeeded = status === "success";
  const Icon = succeeded ? CheckCircle2 : failed ? XCircle : Clock3;
  const title = succeeded ? "Thanh toán đã được xác nhận" : failed ? "Thanh toán chưa hoàn tất" : "Đang xác minh thanh toán";
  const detail = succeeded
    ? "Hệ thống đã nhận xác nhận thanh toán. Bạn có thể theo dõi đơn hàng trong tài khoản."
    : failed
      ? "Khoản thanh toán có thể đã thất bại hoặc hết hạn. Bạn có thể thử lại nếu đơn hàng vẫn cho phép."
      : "Kết quả trên trình duyệt chưa phải bằng chứng thanh toán. DynamicMart đang chờ IPN có chữ ký hợp lệ từ VNPay.";
  return (
    <main className="mx-auto flex min-h-[60vh] max-w-xl items-center px-4 py-12 text-center">
      <section className="w-full rounded-xl border border-border bg-surface p-8">
        <Icon className={`mx-auto size-12 ${succeeded ? "text-emerald-600" : failed ? "text-danger" : "text-amber-500"}`} />
        <h1 className="mt-4 text-2xl font-bold">{title}</h1>
        <p className="mt-3 text-sm leading-6 text-muted">{detail}</p>
        <div className="mt-6 flex justify-center gap-3"><Link className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-slate-50" href="/">Về trang chủ</Link><Link className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground" href="/checkout/payment">Quay lại thanh toán</Link></div>
      </section>
    </main>
  );
}
