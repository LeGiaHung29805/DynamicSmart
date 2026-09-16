import Image from "next/image";
import Link from "next/link";
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ArrowLeft, ArrowRight, ShieldCheck, Star } from "lucide-react";
import { formatVnd } from "@/components/common/Price";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { ProductCard, categories } from "@/features/home";
import { demoProducts } from "@/features/catalog";

export function generateStaticParams() {
  return demoProducts.map((product) => ({ id: product.id }));
}

export async function generateMetadata({ params }: Readonly<{ params: Promise<{ id: string }> }>): Promise<Metadata> {
  const { id } = await params;
  const product = demoProducts.find((item) => item.id === id);
  return { title: product?.name ?? "Sản phẩm không tồn tại" };
}

export default async function ProductDetailPage({ params }: Readonly<{ params: Promise<{ id: string }> }>) {
  const { id } = await params;
  const product = demoProducts.find((item) => item.id === id);
  if (!product) notFound();

  const categoryName = categories.find((item) => item.href.endsWith(`category=${product.category}`))?.name ?? "Sản phẩm";
  const related = demoProducts.filter((item) => item.id !== product.id && item.category === product.category).slice(0, 4);
  const suggestions = related.length ? related : demoProducts.filter((item) => item.id !== product.id).slice(0, 4);

  return (
    <div className="mx-auto w-full max-w-7xl space-y-9 px-4 py-9 sm:px-6 lg:px-8 lg:py-14">
      <Link className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 transition hover:text-brand" href="/products"><ArrowLeft className="size-4" /> Tất cả sản phẩm</Link>
      <div className="grid gap-8 lg:grid-cols-[minmax(0,1.05fr)_minmax(0,.95fr)] lg:gap-12">
        <div className="relative aspect-[4/5] overflow-hidden rounded-[2rem] bg-slate-100 shadow-xl shadow-slate-950/5 sm:aspect-square lg:aspect-[4/5]">
          <Image alt={product.name} className="object-cover" fill priority sizes="(max-width: 1024px) 100vw, 52vw" src={product.image} />
          <span className="absolute top-5 left-5 rounded-full bg-white/95 px-4 py-2 text-xs font-black text-slate-800 shadow-sm">{product.badge}</span>
        </div>
        <div className="space-y-7 lg:py-4">
          <PageHeader eyebrow={`${product.brand} · ${categoryName}`} title={product.name} description="Sản phẩm minh họa để thống nhất giao diện chi tiết; thông tin và tồn kho thật sẽ được lấy từ Catalog API." />
          <div className="flex flex-wrap items-center gap-4 text-sm text-slate-500"><span className="inline-flex items-center gap-1.5 font-bold text-amber-500"><Star className="size-4 fill-current" />{product.rating}</span><span>Đã bán {product.sold}</span><span>Mã: {product.id}</span></div>
          <div className="flex flex-wrap items-end gap-3 border-y border-slate-200 py-6"><span className="text-3xl font-black tracking-tight text-emerald-800">{formatVnd(product.price)}</span>{product.oldPrice > product.price ? <del className="pb-1 text-sm text-slate-400">{formatVnd(product.oldPrice)}</del> : null}</div>
          <SurfacePanel className="space-y-4">
            <div className="flex items-start gap-3"><ShieldCheck className="mt-0.5 size-5 shrink-0 text-brand" /><p className="text-sm leading-6 text-slate-600">Khung này sẵn sàng cho biến thể, số lượng, tồn kho và thông tin giao hàng khi API sản phẩm được nối vào.</p></div>
            <button className="w-full cursor-not-allowed rounded-xl bg-slate-200 px-5 py-3 text-sm font-bold text-slate-500" disabled type="button">Thêm vào giỏ hàng · đang chờ Cart API</button>
          </SurfacePanel>
          <Link className="inline-flex items-center gap-2 text-sm font-bold text-brand hover:underline" href="/products">Tiếp tục khám phá <ArrowRight className="size-4" /></Link>
        </div>
      </div>
      <div className="space-y-5 border-t border-slate-200 pt-9">
        <h2 className="text-2xl font-black tracking-tight text-slate-950">Gợi ý dành cho bạn</h2>
        <div className="grid grid-cols-2 gap-3 sm:gap-5 md:grid-cols-3 lg:grid-cols-4">{suggestions.map((item) => <ProductCard key={item.id} product={item} />)}</div>
      </div>
    </div>
  );
}
