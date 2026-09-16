import type { Metadata } from "next";
import { AdminSectionPage } from "@/features/admin";

export const metadata: Metadata = { title: "Quản lý catalog" };

export default function AdminCatalogPage() {
  return <AdminSectionPage section="catalog" />;
}
