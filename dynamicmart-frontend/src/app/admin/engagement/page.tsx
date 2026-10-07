import type { Metadata } from "next";
import { AdminEngagementPage } from "@/features/engagement";

export const metadata: Metadata = { title: "Tuong tac khach hang" };

export default function AdminEngagementRoutePage() {
  return <AdminEngagementPage />;
}
