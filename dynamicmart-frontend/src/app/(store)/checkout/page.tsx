import type { Metadata } from "next";
import { CheckoutPage as CheckoutFeaturePage } from "@/features/checkout";

export const metadata: Metadata = { title: "Thanh toán" };

export default async function CheckoutPage({ searchParams }: Readonly<{ searchParams: Promise<Record<string, string | string[] | undefined>> }>) {
  const params = await searchParams;
  const value = params.sessionId;
  const sessionId = Array.isArray(value) ? value[0] : value;
  return <CheckoutFeaturePage initialSessionId={sessionId} />;
}
