import type { Metadata } from "next";
import { CustomerSupportPage } from "@/features/engagement";

export const metadata: Metadata = {
  title: "Hỗ trợ & Chat CSKH | DynamicMart",
  description: "Kênh hỗ trợ và tư vấn trực tuyến chính thức từ DynamicMart.",
};

export default function SupportPage() {
  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <CustomerSupportPage />
    </div>
  );
}
