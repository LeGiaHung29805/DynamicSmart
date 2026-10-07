import type { Metadata } from "next";
import { AdminProductListPanel } from "@/features/catalog/components/admin/AdminProductPanel";

export const metadata: Metadata = { title: "Sản phẩm | Catalog" };

export default function AdminProductsPage() {
  return <AdminProductListPanel />;
}
