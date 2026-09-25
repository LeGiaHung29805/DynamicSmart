import type { Metadata } from "next";
import { PromotionAdminPage } from "@/features/promotion";
export const metadata: Metadata = { title: "Quản lý khuyến mãi" };
export default function Page() { return <PromotionAdminPage />; }
