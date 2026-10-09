import type { Metadata } from "next";
import { AdminProductFormPanel } from "@/features/catalog/components/admin/AdminProductPanel";

export const metadata: Metadata = { title: "Tạo sản phẩm | Catalog" };

export default function NewAdminProductPage() {
  return <AdminProductFormPanel />;
}
