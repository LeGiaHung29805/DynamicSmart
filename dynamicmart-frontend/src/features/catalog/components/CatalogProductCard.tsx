import Link from "next/link";
import { ArrowUpRight, BadgePercent, PackageCheck } from "lucide-react";
import { formatVnd } from "@/components/common/Price";
import type { ProductSummary } from "../types";

export function CatalogProductCard({ product }: Readonly<{ product: ProductSummary }>) {
  const price = product.representativePrice;
  const effectivePrice = price.salePriceVnd ?? price.listPriceVnd;
  const hasRange = product.minimumListPriceVnd !== product.maximumListPriceVnd;
  const badge = price.salePriceVnd !== null && price.salePriceVnd !== undefined
    ? `Giảm ${price.directSalePercent ?? 0}%`
    : product.bestSeller ? "Bán chạy" : product.featured ? "Nổi bật" : "Mới";

  return (
    <article className="group overflow-hidden rounded-3xl border border-slate-200/80 bg-white transition duration-300 hover:-translate-y-1 hover:border-emerald-200 hover:shadow-2xl hover:shadow-emerald-950/10">
      <Link className="relative block aspect-[4/5] overflow-hidden bg-gradient-to-br from-slate-100 to-slate-200" href={`/products/${product.slug}`}>
        {product.primaryImage?.imageUrl ? (
          // Ảnh catalog đến từ object storage do backend quản lý, nên không giới hạn hostname ở build time.
          // eslint-disable-next-line @next/next/no-img-element
          <img alt={product.primaryImage.altText ?? product.name} className="size-full object-cover transition duration-500 group-hover:scale-105" loading="lazy" src={product.primaryImage.imageUrl} />
        ) : <span className="grid size-full place-items-center text-sm font-bold text-slate-400">DynamicMart</span>}
        <span className="absolute top-3 left-3 rounded-full bg-white/95 px-2.5 py-1 text-[10px] font-black tracking-wide text-slate-800 uppercase shadow-sm">{badge}</span>
        <span className="absolute right-3 bottom-3 grid size-10 translate-y-2 place-items-center rounded-full bg-slate-950 text-white opacity-0 shadow-lg transition group-hover:translate-y-0 group-hover:opacity-100"><ArrowUpRight className="size-4" /></span>
      </Link>
      <div className="p-4 sm:p-5">
        <Link href={`/products/${product.slug}`}>
          <p className="text-[10px] font-black tracking-[0.16em] text-brand uppercase sm:text-xs">{product.category.name}</p>
          <h3 className="mt-1.5 line-clamp-2 min-h-10 text-sm leading-5 font-bold text-slate-800 transition group-hover:text-brand sm:min-h-11 sm:text-[15px]">{product.name}</h3>
        </Link>
        <div className="mt-3 flex flex-wrap items-end gap-x-2 gap-y-1">
          <span className="text-sm font-black text-slate-950 sm:text-base">{hasRange && !price.salePriceVnd ? `${formatVnd(product.minimumListPriceVnd)} – ${formatVnd(product.maximumListPriceVnd)}` : formatVnd(effectivePrice)}</span>
          {price.salePriceVnd !== null && price.salePriceVnd !== undefined ? <del className="text-[11px] text-slate-400 sm:text-xs">{formatVnd(price.listPriceVnd)}</del> : null}
        </div>
        <div className="mt-3 flex items-center justify-between gap-2 border-t border-slate-100 pt-3 text-[11px] text-slate-500 sm:text-xs">
          <span className="inline-flex items-center gap-1 font-semibold text-emerald-700"><PackageCheck className="size-3.5" /> Còn hàng</span>
          {product.voucherEligible ? <span className="inline-flex items-center gap-1 font-semibold text-amber-700"><BadgePercent className="size-3.5" /> Có voucher</span> : null}
        </div>
      </div>
    </article>
  );
}
