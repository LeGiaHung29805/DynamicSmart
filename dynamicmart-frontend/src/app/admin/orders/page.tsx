import type { Metadata } from "next";
import { AdminSectionPage } from "@/features/admin";

export const metadata: Metadata = { title: "Quản lý đơn hàng" };

export default function AdminOrdersPage() {
  return <AdminSectionPage section="orders" />;
}
