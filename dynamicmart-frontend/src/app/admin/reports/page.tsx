import type { Metadata } from "next";
import { AdminSectionPage } from "@/features/admin";

export const metadata: Metadata = { title: "Báo cáo" };

export default function AdminReportsPage() {
  return <AdminSectionPage section="reports" />;
}
