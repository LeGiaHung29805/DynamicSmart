import type { Metadata } from "next";
import { Suspense } from "react";
import { PaymentResultPanel } from "@/features/payment";

export const metadata: Metadata = { title: "Kết quả thanh toán" };

export default function PaymentResultPage() {
  return <Suspense fallback={<main className="mx-auto max-w-xl px-4 py-12 text-center">Đang đọc kết quả thanh toán…</main>}><PaymentResultPanel /></Suspense>;
}
