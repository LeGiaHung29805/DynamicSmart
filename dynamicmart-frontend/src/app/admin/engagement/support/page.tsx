import type { Metadata } from "next";
import { AdminEngagementPage } from "@/features/engagement";

export const metadata: Metadata = { title: "Hỗ trợ khách hàng | DynamicMart Admin" };

export default function AdminSupportPage() {
  return <AdminEngagementPage />;
}
