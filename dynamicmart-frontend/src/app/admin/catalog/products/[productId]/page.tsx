import type { Metadata } from "next";
import { AdminProductDetailPanel } from "@/features/catalog/components/admin/AdminProductPanel";

export const metadata: Metadata = { title: "Chi tiết sản phẩm | Catalog" };

export default async function AdminProductDetailPage({ params, searchParams }: Readonly<{
  params: Promise<{ productId: string }>;
  searchParams: Promise<{ step?: string | string[] }>;
}>) {
  const [{ productId }, query] = await Promise.all([params, searchParams]);
  const initialStep = Array.isArray(query.step) ? query.step[0] : query.step;
  return <AdminProductDetailPanel initialStep={initialStep} productId={productId} />;
}
