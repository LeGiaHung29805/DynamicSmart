import type { Metadata } from "next";
import { AdminInventoryPanel } from "@/features/catalog/components/admin/AdminInventoryPanel";

export const metadata: Metadata = { title: "Tồn kho | Catalog" };

export default function AdminInventoryPage() {
  return <AdminInventoryPanel />;
}
