import type { Metadata } from "next";
import { AdminDashboard } from "@/features/admin";

export const metadata: Metadata = { title: "Tổng quan quản trị" };

export default function AdminPage() {
  return <AdminDashboard />;
}
