import { AdminOrderDetailPage } from "@/features/admin/components/AdminDetailPages";

export default async function AdminOrderDetailRoute({ params }: Readonly<{ params: Promise<{ orderId: string }> }>) {
  const { orderId } = await params;
  return <AdminOrderDetailPage orderId={orderId} />;
}
