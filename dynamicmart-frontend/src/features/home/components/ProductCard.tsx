import Image from "next/image";
import Link from "next/link";
import { Heart, ShoppingBag } from "lucide-react";
import { formatVnd } from "@/components/common/Price";

type ProductCardProps = { product: { badge: string; brand: string; id: string; image: string; name: string; oldPrice: number; price: number; rating: number; sold: string } };

export function ProductCard({ product }: Readonly<ProductCardProps>) {
  return (
    <article className="group overflow-hidden rounded-3xl border border-slate-200/80 bg-white transition duration-300 hover:-translate-y-1.5 hover:border-emerald-200 hover:shadow-2xl hover:shadow-emerald-950/10">
      <div className="relative aspect-[4/5] overflow-hidden bg-slate-100">
        <Link className="block size-full" href={`/products/${product.id}`}>
          <Image alt={product.name} className="object-cover transition duration-500 group-hover:scale-105" fill sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 25vw" src={product.image} />
          <span className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black/30 to-transparent opacity-0 transition group-hover:opacity-100" />
        </Link>
        <span className="absolute top-3 left-3 rounded-full bg-white/95 px-2.5 py-1 text-[10px] font-black tracking-wide text-slate-800 uppercase shadow-sm backdrop-blur">{product.badge}</span>
        <button aria-label="Lưu sản phẩm" className="absolute top-3 right-3 grid size-9 place-items-center rounded-full bg-white/90 text-slate-600 shadow-sm backdrop-blur transition hover:scale-105 hover:bg-white hover:text-rose-500" type="button"><Heart className="size-4" /></button>
        <Link className="absolute right-3 bottom-3 grid size-10 translate-y-3 place-items-center rounded-full bg-slate-950 text-white opacity-0 shadow-lg transition duration-300 group-hover:translate-y-0 group-hover:opacity-100 hover:bg-brand" href={`/products/${product.id}`}><ShoppingBag className="size-4" /><span className="sr-only">Xem sản phẩm</span></Link>
      </div>
      <div className="p-4 sm:p-5">
        <Link href={`/products/${product.id}`}>
          <p className="text-[10px] font-black tracking-[0.16em] text-brand uppercase sm:text-xs">{product.brand}</p>
          <h3 className="mt-1.5 line-clamp-2 min-h-10 text-sm leading-5 font-bold text-slate-800 transition group-hover:text-brand sm:min-h-11 sm:text-[15px]">{product.name}</h3>
        </Link>
        <div className="mt-3 flex flex-wrap items-end gap-x-2 gap-y-1"><span className="text-sm font-black text-slate-950 sm:text-base">{formatVnd(product.price)}</span>{product.oldPrice > product.price ? <del className="text-[11px] text-slate-400 sm:text-xs">{formatVnd(product.oldPrice)}</del> : null}</div>
        <div className="mt-3 flex items-center justify-between border-t border-slate-100 pt-3 text-[11px] text-slate-500 sm:text-xs"><span className="font-bold text-amber-500">★ {product.rating}</span><span>Đã bán {product.sold}</span></div>
      </div>
    </article>
  );
}
