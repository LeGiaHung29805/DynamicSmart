"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { CheckCircle2, LogIn, PackageCheck } from "lucide-react";
import { formatVnd } from "@/components/common/Price";
import { LoadingState } from "@/components/common/PageState";
import { apiClient } from "@/lib/api/client";
import { isApiError } from "@/lib/api/error";
import { useAuthSession } from "@/lib/auth/session";

interface DemoCheckoutSession {
  id: string;
  variantId: string;
  sku: string;
  productName: string;
  variantName?: string | null;
  quantity: number;
  unitPriceVnd: number;
  lineTotalVnd: number;
  createdAt: string;
}

export function StandaloneCheckoutPage() {
  const searchParams = useSearchParams();
  const auth = useAuthSession();
  const sessionId = searchParams.get("sessionId");
  const [checkout, setCheckout] = useState<DemoCheckoutSession>();
  const [error, setError] = useState<string>();

  useEffect(() => {
    if (auth.status !== "authenticated" || !sessionId) return;
    void apiClient.get<DemoCheckoutSession>(
      `/api/v1/catalog/demo/checkout/sessions/${encodeURIComponent(sessionId)}`,
    ).then(setCheckout).catch((caught) => {
      setError(isApiError(caught) ? caught.message : "Không thể tải phiên Mua ngay.");
    });
  }, [auth.status, sessionId]);

  if (auth.status === "loading") return <LoadingState title="Đang khôi phục phiên demo..." />;
  if (auth.status === "anonymous") {
    return <div className="rounded-3xl border border-amber-200 bg-amber-50 p-8 text-center"><LogIn className="mx-auto size-9 text-amber-700" /><h1 className="mt-4 text-2xl font-black">Cần đăng nhập lại</h1><Link className="mt-5 inline-flex rounded-xl bg-emerald-950 px-5 py-3 text-sm font-bold text-white" href={`/login?returnTo=${encodeURIComponent(`/catalog-demo/checkout?sessionId=${sessionId ?? ""}`)}`}>Đăng nhập demo</Link></div>;
  }
  if (!sessionId) return <p className="rounded-2xl bg-red-50 p-5 text-danger">Thiếu mã phiên Mua ngay.</p>;
  if (error) return <p className="rounded-2xl bg-red-50 p-5 text-danger">{error}</p>;
  if (!checkout) return <LoadingState title="Đang tải phiên Mua ngay..." />;

  return (
    <div className="overflow-hidden rounded-3xl border border-emerald-200 bg-white shadow-xl shadow-emerald-950/5">
      <div className="bg-emerald-950 p-7 text-white">
        <CheckCircle2 className="size-10 text-emerald-300" />
        <h1 className="mt-4 text-3xl font-black">Catalog đã bàn giao hợp lệ</h1>
        <p className="mt-2 text-sm text-emerald-100">Phiên này chứng minh luồng chọn Variant, kiểm tra giá và tồn kho hoạt động độc lập. Chưa tạo Order hay trừ kho thật.</p>
      </div>
      <div className="space-y-5 p-7">
        <div className="flex items-start gap-4"><PackageCheck className="mt-1 size-6 text-brand" /><div><p className="font-black text-slate-950">{checkout.productName}</p><p className="mt-1 text-sm text-slate-500">{checkout.variantName ?? checkout.sku} · SKU {checkout.sku}</p></div></div>
        <dl className="grid gap-3 rounded-2xl bg-slate-50 p-5 text-sm sm:grid-cols-3"><div><dt className="text-slate-500">Số lượng</dt><dd className="mt-1 font-black">{checkout.quantity}</dd></div><div><dt className="text-slate-500">Đơn giá</dt><dd className="mt-1 font-black">{formatVnd(checkout.unitPriceVnd)}</dd></div><div><dt className="text-slate-500">Tạm tính</dt><dd className="mt-1 font-black text-emerald-800">{formatVnd(checkout.lineTotalVnd)}</dd></div></dl>
        <Link className="inline-flex rounded-xl border border-slate-200 px-5 py-3 text-sm font-bold text-slate-700 hover:bg-slate-50" href="/products">Tiếp tục xem sản phẩm</Link>
      </div>
    </div>
  );
}
