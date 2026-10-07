import type { Metadata } from "next";
import { AdminProductFormPanel } from "@/features/catalog/components/admin/AdminProductPanel";

export const metadata: Metadata = { title: "Chỉnh sửa sản phẩm | Catalog" };

export default async function EditAdminProductPage({ params }: Readonly<{
  params: Promise<{ productId: string }>;
}>) {
  const { productId } = await params;
  return <AdminProductFormPanel productId={productId} />;
}
