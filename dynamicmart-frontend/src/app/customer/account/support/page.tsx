import type { Metadata } from "next";
import { CustomerSupportPage } from "@/features/engagement";

export const metadata: Metadata = { title: "Ho tro" };

export default function SupportPage() {
  return <CustomerSupportPage />;
}
