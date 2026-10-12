import type { Metadata } from "next";
import { AdminEngagementPage } from "@/features/engagement";

export const metadata: Metadata = { title: "Hỏi đáp sản phẩm | DynamicMart Admin" };

export default function AdminQuestionsPage() {
  return <AdminEngagementPage />;
}
