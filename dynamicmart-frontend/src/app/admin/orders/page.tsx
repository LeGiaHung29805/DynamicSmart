import type { Metadata } from "next";
import { AdminOrderWorkspace } from "@/features/order";

export const metadata: Metadata = { title: "Quản lý đơn hàng" };

export default function AdminOrdersPage() {
  return <AdminOrderWorkspace />;
}
