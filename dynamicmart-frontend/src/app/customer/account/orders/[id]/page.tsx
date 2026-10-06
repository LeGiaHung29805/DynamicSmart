import type { Metadata } from "next";
import { CustomerOrderDetail } from "@/features/order";

export const metadata: Metadata = { title: "Chi tiết đơn hàng" };

export default async function CustomerOrderDetailPage({ params }: Readonly<{ params: Promise<{ id: string }> }>) {
  const { id } = await params;
  return <CustomerOrderDetail orderId={id} />;
}
