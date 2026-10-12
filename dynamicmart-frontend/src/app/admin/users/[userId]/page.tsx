import { AdminUserDetailPage } from "@/features/admin/components/AdminDetailPages";

export default async function AdminUserDetailRoute({ params }: Readonly<{ params: Promise<{ userId: string }> }>) {
  const { userId } = await params;
  return <AdminUserDetailPage userId={userId} />;
}
