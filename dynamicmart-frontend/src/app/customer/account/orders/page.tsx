import type { Metadata } from "next";
import { CustomerOrderList } from "@/features/order";

export const metadata: Metadata = { title: "Đơn hàng của tôi" };

export default function CustomerOrdersPage() {
  return <CustomerOrderList />;
}
