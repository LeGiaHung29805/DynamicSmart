import type { Metadata } from "next";
import { CheckoutPaymentWizard } from "@/features/payment";

export const metadata: Metadata = { title: "Thanh toán" };

export default function CheckoutPaymentPage() {
  return <CheckoutPaymentWizard />;
}
