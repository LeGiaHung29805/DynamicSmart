import type { Metadata } from "next";
import { AdminPaymentsPage } from "@/features/payment";

export const metadata: Metadata = { title: "Quản lý thanh toán" };

export default function AdminPaymentRoute() {
  return <AdminPaymentsPage />;
}
