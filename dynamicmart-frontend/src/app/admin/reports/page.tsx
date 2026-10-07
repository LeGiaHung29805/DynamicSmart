import type { Metadata } from "next";
import { AdminReportDashboard } from "@/features/engagement";

export const metadata: Metadata = { title: "Báo cáo" };

export default function AdminReportsPage() {
  return <AdminReportDashboard />;
}
