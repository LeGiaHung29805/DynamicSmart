import type { Metadata } from "next";
import { AdminEngagementPage } from "@/features/engagement";

export const metadata: Metadata = { title: "Đánh giá sản phẩm | DynamicMart Admin" };

export default function AdminReviewsPage() {
  return <AdminEngagementPage />;
}
