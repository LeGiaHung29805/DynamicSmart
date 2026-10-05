"use client";

import Link from "next/link";
import { Heart } from "lucide-react";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { CatalogProductCard } from "@/features/catalog/components/CatalogProductCard";
import type { ProductDetail, ProductSummary } from "@/features/catalog/types";
import { apiClient } from "@/lib/api/client";
import { useAuthSession } from "@/lib/auth/session";
import { useWishlist } from "./WishlistProvider";

function toSummary(product: ProductDetail): ProductSummary | null {
  const variant = product.variants.find((item) => item.purchasable) ?? product.variants[0];
  if (!variant) return null;
  const prices = product.variants.map((item) => item.price.listPriceVnd);
  return { id: product.id, name: product.name, slug: product.slug, shortDescription: product.shortDescription,
    category: product.category, primaryImage: product.images.find((image) => image.primary) ?? product.images[0] ?? variant.images[0],
    representativeVariantId: variant.id, representativePrice: variant.price,
    minimumListPriceVnd: Math.min(...prices), maximumListPriceVnd: Math.max(...prices),
    inStock: product.variants.some((item) => item.purchasable), featured: product.featured,
    bestSeller: false, voucherEligible: false, publishedAt: product.publishedAt };
}

export function WishlistPage() {
  const session = useAuthSession();
  const { items, loading } = useWishlist();
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [productsLoading, setProductsLoading] = useState(false);

  useEffect(() => {
    if (session.status !== "authenticated" || loading || items.length === 0) return;
    let cancelled = false;
    void (async () => {
      setProductsLoading(true);
      const values = await Promise.all(items.map((item) => apiClient.get<ProductDetail>(`/api/v1/catalog/products/id/${item.productId}`).catch(() => null)));
        if (!cancelled) setProducts(values.flatMap((value) => value ? [toSummary(value)].filter((item): item is ProductSummary => item !== null) : []));
      if (!cancelled) setProductsLoading(false);
    })();
    return () => { cancelled = true; };
  }, [items, loading, session.status]);

  return <div className="space-y-7">
    <PageHeader eyebrow="Mua sắm" title="Sản phẩm yêu thích" description="Lưu lại sản phẩm bạn quan tâm để dễ dàng xem và mua sau." />
    {session.status === "anonymous" ? <SurfacePanel className="text-center"><Heart className="mx-auto size-10 text-rose-500" /><h2 className="mt-4 text-lg font-black text-slate-900">Đăng nhập để xem Yêu thích</h2><Link className="mt-5 inline-flex rounded-xl bg-emerald-950 px-5 py-3 text-sm font-black text-white" href="/login?returnTo=%2Fwishlist">Đăng nhập</Link></SurfacePanel>
      : loading || productsLoading || session.status === "loading" ? <SurfacePanel><p className="text-sm text-slate-500">Đang tải sản phẩm yêu thích…</p></SurfacePanel>
      : items.length === 0 || products.length === 0 ? <SurfacePanel className="text-center"><Heart className="mx-auto size-10 text-slate-300" /><h2 className="mt-4 text-lg font-black text-slate-900">Chưa có sản phẩm yêu thích</h2><p className="mt-2 text-sm text-slate-500">Nhấn biểu tượng trái tim trên sản phẩm để lưu tại đây.</p><Link className="mt-5 inline-flex rounded-xl border border-emerald-800 px-5 py-3 text-sm font-black text-emerald-900" href="/products">Khám phá sản phẩm</Link></SurfacePanel>
      : <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">{products.map((product) => <CatalogProductCard key={product.id} product={product} />)}</div>}
  </div>;
}
