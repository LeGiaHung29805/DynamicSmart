import type { Metadata } from "next";
import { CustomerOrderDetail } from "@/features/order/components/CustomerOrderDetail";

export const metadata: Metadata = { title: "Chi tiết đơn hàng" };

export default async function CustomerOrderPage({ params }: Readonly<{ params: Promise<{ id: string }> }>) {
  const { id } = await params;
  return <CustomerOrderDetail orderId={id} />;
}
