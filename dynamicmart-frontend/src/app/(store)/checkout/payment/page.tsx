import type { Metadata } from "next";
import { redirect } from "next/navigation";

export const metadata: Metadata = { title: "Thanh toán" };

export default async function CheckoutPaymentPage({ searchParams }: Readonly<{ searchParams: Promise<Record<string, string | string[] | undefined>> }>) {
  const params = await searchParams;
  const value = params.sessionId;
  const sessionId = Array.isArray(value) ? value[0] : value;
  redirect(sessionId ? `/checkout?sessionId=${encodeURIComponent(sessionId)}` : "/checkout");
}
