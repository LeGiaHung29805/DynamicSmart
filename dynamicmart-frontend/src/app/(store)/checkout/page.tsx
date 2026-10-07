import type { Metadata } from "next";
import { CheckoutPage as CheckoutExperience } from "@/features/checkout";

export const metadata: Metadata = { title: "Thanh toán" };

export default async function CheckoutPage({
  searchParams,
}: Readonly<{ searchParams: Promise<{ sessionId?: string }> }>) {
  const { sessionId } = await searchParams;
  return <CheckoutExperience initialSessionId={sessionId} />;
}
