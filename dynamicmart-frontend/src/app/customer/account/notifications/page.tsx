import type { Metadata } from "next";
import { CustomerNotificationsPage } from "@/features/engagement";

export const metadata: Metadata = { title: "Thong bao" };

export default function NotificationsPage() {
  return <CustomerNotificationsPage />;
}
