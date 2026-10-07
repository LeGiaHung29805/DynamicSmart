import type { Metadata } from "next";
import { AdminProductDetailPanel } from "@/features/catalog/components/admin/AdminProductPanel";

export const metadata: Metadata = { title: "Chi tiết sản phẩm | Catalog" };

export default async function AdminProductDetailPage({ params }: Readonly<{
  params: Promise<{ productId: string }>;
}>) {
  const { productId } = await params;
  return <AdminProductDetailPanel productId={productId} />;
}
