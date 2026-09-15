import { AdminLayout } from "@/components/layouts/AdminLayout";

export default function AdminRouteLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <AdminLayout>{children}</AdminLayout>;
}
