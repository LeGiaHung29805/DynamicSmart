import type { Metadata } from "next";
import { AdminAttributePanel } from "@/features/catalog/components/admin/AdminAttributePanel";

export const metadata: Metadata = { title: "Thương hiệu & thuộc tính | Catalog" };

export default function AdminAttributesPage() {
  return <AdminAttributePanel />;
}
