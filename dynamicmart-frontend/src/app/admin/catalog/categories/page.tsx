import type { Metadata } from "next";
import { AdminCategoryPanel } from "@/features/catalog/components/admin/AdminCategoryPanel";

export const metadata: Metadata = { title: "Danh mục | Catalog" };

export default function AdminCategoriesPage() {
  return <AdminCategoryPanel />;
}
