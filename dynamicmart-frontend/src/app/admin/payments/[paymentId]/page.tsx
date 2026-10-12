import { AdminPaymentDetailPage } from "@/features/admin/components/AdminDetailPages";

export default async function AdminPaymentDetailRoute({ params }: Readonly<{ params: Promise<{ paymentId: string }> }>) {
  const { paymentId } = await params;
  return <AdminPaymentDetailPage paymentId={paymentId} />;
}
