import type { Metadata } from "next";
import { AdminCatalogWorkspace } from "@/features/catalog/components/admin/AdminCatalogWorkspace";

export const metadata: Metadata = { title: "Quản lý catalog" };

export default function AdminCatalogPage() {
  return <AdminCatalogWorkspace />;
}
