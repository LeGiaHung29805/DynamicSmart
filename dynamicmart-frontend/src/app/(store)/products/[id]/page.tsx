import Link from "next/link";
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ArrowLeft, MessageSquareText } from "lucide-react";
import { ErrorState } from "@/components/common/PageState";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { getProductDetail, getProducts } from "@/features/catalog/api/catalog.api";
import { CatalogProductCard } from "@/features/catalog/components/CatalogProductCard";
import { ProductDetailExperience, ProductSpecifications } from "@/features/catalog/components/ProductDetailExperience";
import { isApiError } from "@/lib/api/error";

export async function generateMetadata({ params }: Readonly<{ params: Promise<{ id: string }> }>): Promise<Metadata> {
  try {
    const { id } = await params;
    const product = await getProductDetail(id);
    return { title: product.name, description: product.shortDescription ?? undefined };
  } catch {
    return { title: "Chi tiết sản phẩm" };
  }
}

export default async function ProductDetailPage({ params, searchParams }: Readonly<{
  params: Promise<{ id: string }>;
  searchParams: Promise<{ variant?: string; quantity?: string; resumeUntil?: string }>;
}>) {
  const [{ id }, query] = await Promise.all([params, searchParams]);
  let product;
  try {
    product = await getProductDetail(id);
  } catch (error) {
    if (isApiError(error) && error.status === 404) notFound();
  }
  if (!product) {
    return <div className="mx-auto w-full max-w-7xl px-4 py-12 sm:px-6 lg:px-8"><ErrorState description="Không thể tải thông tin sản phẩm. Hãy kiểm tra Catalog API rồi thử lại." title="Chi tiết sản phẩm chưa sẵn sàng" /></div>;
  }
  const related = await getProducts({ categoryId: product.category.id, size: 5 }).catch(() => null);
  const suggestions = related?.content.filter((item) => item.id !== product.id).slice(0, 4) ?? [];
  const initialQuantity = Math.max(Number.parseInt(query.quantity ?? "1", 10) || 1, 1);

  return (
    <div className="mx-auto w-full max-w-7xl space-y-10 px-4 py-9 sm:px-6 lg:px-8 lg:py-14">
      <Link className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 transition hover:text-brand" href="/products"><ArrowLeft className="size-4" /> Tất cả sản phẩm</Link>
      <ProductDetailExperience initialQuantity={initialQuantity} initialVariantId={query.variant} product={product} />
      <div className="grid gap-6 border-t border-slate-200 pt-9 lg:grid-cols-[1.3fr_.7fr]">
        <section className="space-y-5"><div><p className="text-xs font-black tracking-[0.18em] text-brand uppercase">Thông tin sản phẩm</p><h2 className="mt-2 text-2xl font-black text-slate-950">Mô tả & thông số</h2></div>{product.description ? <p className="whitespace-pre-line text-sm leading-7 text-slate-600">{product.description}</p> : null}<ProductSpecifications attributes={product.attributes} /></section>
        <SurfacePanel className="h-fit"><MessageSquareText className="size-6 text-brand" /><h2 className="mt-4 text-lg font-black">Đánh giá sản phẩm</h2><p className="mt-2 text-sm leading-6 text-slate-500">Đánh giá đã duyệt sẽ hiển thị tại đây khi Engagement API của Tùng được nối vào contract chung.</p></SurfacePanel>
      </div>
      {suggestions.length ? <section className="space-y-5 border-t border-slate-200 pt-9"><h2 className="text-2xl font-black tracking-tight text-slate-950">Sản phẩm cùng danh mục</h2><div className="grid grid-cols-2 gap-3 sm:gap-5 md:grid-cols-3 lg:grid-cols-4">{suggestions.map((item) => <CatalogProductCard key={item.id} product={item} />)}</div></section> : null}
    </div>
  );
}
