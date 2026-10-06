import { redirect } from "next/navigation";

export default async function CustomerOrderPage({ params }: Readonly<{ params: Promise<{ id: string }> }>) {
  const { id } = await params;
  redirect(`/customer/account/orders/${encodeURIComponent(id)}`);
}
