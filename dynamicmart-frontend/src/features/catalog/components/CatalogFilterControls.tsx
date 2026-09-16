"use client";

import { FormEvent, ReactNode, useRef, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Check, ChevronDown, Search, SlidersHorizontal, X } from "lucide-react";
import { brandOptions, categoryOptions, matchesPrice, priceOptions } from "../filterOptions";
import { demoProducts } from "../demoProducts";
import type { CatalogFilters } from "../types";

type Draft = { q: string; category: string; brand: string; price: string; promotion: boolean };

function matchingCount(draft: Draft): number {
  const query = draft.q.trim().toLocaleLowerCase("vi-VN");
  return demoProducts.filter((product) =>
    (!query || `${product.name} ${product.brand}`.toLocaleLowerCase("vi-VN").includes(query))
    && (!draft.category || product.category === draft.category)
    && (!draft.brand || product.brand.toLocaleLowerCase("vi-VN") === draft.brand)
    && (!draft.price || matchesPrice(product.price, draft.price))
    && (!draft.promotion || product.oldPrice > product.price)
  ).length;
}

function facetCount(draft: Draft, facet: "category" | "brand" | "price", value: string): number {
  return matchingCount({ ...draft, [facet]: value });
}

function fromFilters(filters: CatalogFilters): Draft {
  return {
    q: filters.q ?? "", category: filters.category ?? "", brand: filters.brand ?? "",
    price: filters.price ?? "", promotion: filters.promotion === "true",
  };
}

function filterUrl(draft: Draft, sort: string): string {
  const params = new URLSearchParams();
  if (draft.q.trim()) params.set("q", draft.q.trim());
  if (draft.category) params.set("category", draft.category);
  if (draft.brand) params.set("brand", draft.brand);
  if (draft.price) params.set("price", draft.price);
  if (draft.promotion) params.set("promotion", "true");
  if (sort && sort !== "featured") params.set("sort", sort);
  return `/products${params.size ? `?${params.toString()}` : ""}`;
}

interface FacetRowProps {
  count?: number;
  label: string;
  onClick: () => void;
  selected: boolean;
}

function FacetRow({ count, label, onClick, selected }: Readonly<FacetRowProps>) {
  return (
    <button
      aria-pressed={selected}
      className={`flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm transition ${selected ? "bg-emerald-50 font-bold text-emerald-900" : "text-slate-600 hover:bg-slate-50 hover:text-slate-950"}`}
      onClick={onClick}
      type="button"
    >
      <span className={`grid size-[18px] shrink-0 place-items-center rounded-full border ${selected ? "border-brand bg-brand text-white" : "border-slate-300 bg-white"}`}>{selected ? <Check className="size-3" /> : null}</span>
      <span className="min-w-0 flex-1">{label}</span>
      {count !== undefined ? <span className="text-xs tabular-nums text-slate-400">{count}</span> : null}
    </button>
  );
}

function FacetGroup({ title, children }: Readonly<{ title: string; children: ReactNode }>) {
  return (
    <details className="group border-b border-slate-100 py-5 last:border-b-0" open>
      <summary className="flex cursor-pointer list-none items-center justify-between text-sm font-black text-slate-900 marker:hidden [&::-webkit-details-marker]:hidden">
        {title}<ChevronDown className="size-4 text-slate-400 transition group-open:rotate-180" />
      </summary>
      <div className="mt-3 space-y-0.5">{children}</div>
    </details>
  );
}

function FacetGroups({ draft, setDraft }: Readonly<{ draft: Draft; setDraft: (next: Draft) => void }>) {
  return (
    <div>
      <FacetGroup title="Danh mục">
        <FacetRow label="Tất cả danh mục" onClick={() => setDraft({ ...draft, category: "" })} selected={!draft.category} />
        {categoryOptions.map((item) => <FacetRow count={facetCount(draft, "category", item.value)} key={item.value} label={item.label} onClick={() => setDraft({ ...draft, category: item.value })} selected={draft.category === item.value} />)}
      </FacetGroup>
      <FacetGroup title="Thương hiệu">
        <FacetRow label="Tất cả thương hiệu" onClick={() => setDraft({ ...draft, brand: "" })} selected={!draft.brand} />
        {brandOptions.map((item) => <FacetRow count={facetCount(draft, "brand", item.value)} key={item.value} label={item.label} onClick={() => setDraft({ ...draft, brand: item.value })} selected={draft.brand === item.value} />)}
      </FacetGroup>
      <FacetGroup title="Khoảng giá">
        <FacetRow label="Mọi mức giá" onClick={() => setDraft({ ...draft, price: "" })} selected={!draft.price} />
        {priceOptions.filter((item) => item.count > 0).map((item) => <FacetRow count={facetCount(draft, "price", item.value)} key={item.value} label={item.label} onClick={() => setDraft({ ...draft, price: item.value })} selected={draft.price === item.value} />)}
      </FacetGroup>
      <div className="py-5">
        <button aria-pressed={draft.promotion} className={`flex w-full items-center justify-between rounded-xl p-3 text-left text-sm font-bold transition ${draft.promotion ? "bg-amber-50 text-amber-900" : "bg-slate-50 text-slate-700 hover:bg-amber-50"}`} onClick={() => setDraft({ ...draft, promotion: !draft.promotion })} type="button">
          <span><span className="block">Đang giảm giá</span><span className="mt-0.5 block text-xs font-normal text-slate-500">Ưu đãi trên bộ sản phẩm mẫu</span></span>
          <span className={`relative h-6 w-11 rounded-full transition ${draft.promotion ? "bg-amber-500" : "bg-slate-300"}`}><span className={`absolute top-1 size-4 rounded-full bg-white shadow-sm transition ${draft.promotion ? "left-6" : "left-1"}`} /></span>
        </button>
      </div>
    </div>
  );
}

export function CatalogFilterControls({ filters, count, children }: Readonly<{ filters: CatalogFilters; count: number; children: ReactNode }>) {
  const router = useRouter();
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [draft, setDraft] = useState<Draft>(() => fromFilters(filters));
  const [pending, startTransition] = useTransition();
  const applied = fromFilters(filters);
  const sort = filters.sort ?? "featured";
  const activeCount = Number(Boolean(applied.q)) + Number(Boolean(applied.category)) + Number(Boolean(applied.brand)) + Number(Boolean(applied.price)) + Number(applied.promotion);
  const draftCount = matchingCount(draft);

  function navigate(next: Draft, nextSort = sort) {
    dialogRef.current?.close();
    startTransition(() => router.push(filterUrl(next, nextSort)));
  }

  function apply(event?: FormEvent) {
    event?.preventDefault();
    navigate(draft);
  }

  function clearAll() {
    const empty = fromFilters({});
    setDraft(empty);
    navigate(empty, "featured");
  }

  function removeFilter(key: keyof Draft) {
    const next = { ...applied };
    if (key === "promotion") next.promotion = false;
    else next[key] = "";
    navigate(next);
  }

  const chips = [
    applied.q ? { label: `“${applied.q}”`, key: "q" } : null,
    applied.category ? { label: categoryOptions.find((item) => item.value === applied.category)?.label ?? applied.category, key: "category" } : null,
    applied.brand ? { label: brandOptions.find((item) => item.value === applied.brand)?.label ?? applied.brand, key: "brand" } : null,
    applied.price ? { label: priceOptions.find((item) => item.value === applied.price)?.label ?? applied.price, key: "price" } : null,
    applied.promotion ? { label: "Đang giảm giá", key: "promotion" } : null,
  ].filter((chip): chip is { label: string; key: keyof Draft } => chip !== null);

  return (
    <>
      <div className="grid items-start gap-5 lg:grid-cols-[280px_minmax(0,1fr)] lg:gap-7">
        <div className="min-w-0 space-y-4 lg:col-start-2 lg:row-start-1">
        <div className="flex flex-col gap-3 rounded-[1.75rem] border border-slate-200/80 bg-white p-3 shadow-sm shadow-slate-950/[0.03] md:flex-row md:items-center md:justify-between">
          <form className="flex min-w-0 flex-1 items-center gap-3 rounded-2xl bg-slate-50 px-4 py-2 ring-1 ring-transparent transition focus-within:bg-white focus-within:ring-emerald-300" onSubmit={apply}>
            <Search className="size-5 shrink-0 text-slate-400" />
            <label className="sr-only" htmlFor="catalog-search">Tìm sản phẩm</label>
            <input className="min-w-0 flex-1 bg-transparent py-1.5 text-sm outline-none placeholder:text-slate-400" id="catalog-search" onChange={(event) => setDraft({ ...draft, q: event.target.value })} placeholder="Tìm sản phẩm, thương hiệu..." value={draft.q} />
            <button className="hidden rounded-xl bg-emerald-950 px-5 py-2 text-sm font-bold text-white transition hover:bg-brand sm:block" disabled={pending} type="submit">Tìm</button>
          </form>
          <div className="flex items-center gap-2">
            <button className="inline-flex min-h-11 flex-1 items-center justify-center gap-2 rounded-xl border border-slate-200 px-4 text-sm font-bold text-slate-800 transition hover:border-emerald-300 hover:text-brand lg:hidden" onClick={() => dialogRef.current?.showModal()} type="button"><SlidersHorizontal className="size-4" />Bộ lọc{activeCount ? <span className="grid size-5 place-items-center rounded-full bg-brand text-[11px] text-white">{activeCount}</span> : null}</button>
            <label className="inline-flex min-h-11 flex-1 items-center gap-2 rounded-xl border border-slate-200 px-3 text-sm font-semibold text-slate-600 md:flex-none">
              <span className="hidden sm:inline">Sắp xếp:</span>
              <select aria-label="Sắp xếp sản phẩm" className="min-w-0 flex-1 cursor-pointer bg-transparent text-sm font-bold text-slate-900 outline-none" onChange={(event) => navigate(applied, event.target.value)} value={sort}>
                <option value="featured">Nổi bật</option><option value="newest">Mới nhất</option><option value="best-selling">Bán chạy</option><option value="price-asc">Giá tăng dần</option><option value="price-desc">Giá giảm dần</option>
              </select>
            </label>
          </div>
        </div>
        <div aria-label="Danh mục nhanh" className="flex gap-2 overflow-x-auto pb-1">
          <button aria-pressed={!applied.category} className={`shrink-0 rounded-full px-4 py-2 text-xs font-bold transition ${!applied.category ? "bg-emerald-950 text-white" : "border border-slate-200 bg-white text-slate-600 hover:border-emerald-300 hover:text-brand"}`} onClick={() => navigate({ ...applied, category: "" })} type="button">Tất cả</button>
          {categoryOptions.map((item) => <button aria-pressed={applied.category === item.value} className={`shrink-0 rounded-full px-4 py-2 text-xs font-bold transition ${applied.category === item.value ? "bg-emerald-950 text-white" : "border border-slate-200 bg-white text-slate-600 hover:border-emerald-300 hover:text-brand"}`} key={item.value} onClick={() => navigate({ ...applied, category: item.value })} type="button">{item.label}</button>)}
        </div>
        {chips.length ? <div aria-label="Bộ lọc đang áp dụng" className="flex flex-wrap items-center gap-2"><span className="mr-1 text-xs font-semibold text-slate-500">Đang lọc:</span>{chips.map((chip) => <button aria-label={`Bỏ bộ lọc ${chip.label}`} className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-xs font-bold text-emerald-900 transition hover:bg-emerald-100" key={chip.key} onClick={() => removeFilter(chip.key)} type="button">{chip.label}<X className="size-3" /></button>)}<button className="px-2 py-1.5 text-xs font-bold text-slate-500 hover:text-brand" onClick={clearAll} type="button">Xóa tất cả</button></div> : null}
        </div>
        <aside aria-label="Bộ lọc sản phẩm" className="hidden lg:col-start-1 lg:row-start-1 lg:row-span-2 lg:block">
          <div className="sticky top-24 overflow-hidden rounded-[1.75rem] border border-slate-200/80 bg-white shadow-sm shadow-slate-950/[0.03]">
            <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4"><div><p className="text-[11px] font-black tracking-[0.15em] text-brand uppercase">Tùy chọn mua sắm</p><h2 className="mt-1 text-lg font-black text-slate-950">Lọc sản phẩm</h2></div>{activeCount ? <span className="grid size-7 place-items-center rounded-full bg-emerald-50 text-xs font-black text-brand">{activeCount}</span> : <SlidersHorizontal className="size-4 text-slate-400" />}</div>
            <div className="filter-sidebar-scroll max-h-[calc(100vh-15rem)] overflow-y-auto px-5"><FacetGroups draft={draft} setDraft={setDraft} /></div>
            <div className="border-t border-slate-100 bg-white p-4"><button className="w-full rounded-xl bg-emerald-950 px-4 py-3 text-sm font-bold text-white transition hover:bg-brand disabled:opacity-60" disabled={pending} onClick={() => apply()} type="button">{pending ? "Đang lọc..." : `Xem ${draftCount} sản phẩm`}</button>{activeCount ? <button className="mt-2 w-full py-1 text-xs font-bold text-slate-500 transition hover:text-brand" onClick={clearAll} type="button">Xóa tất cả bộ lọc</button> : null}</div>
          </div>
        </aside>
        <div className="min-w-0 space-y-5 lg:col-start-2 lg:row-start-2">
          <div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-xl font-black text-slate-950">Lựa chọn dành cho bạn</p><p className="mt-1 text-sm text-slate-500">{count} sản phẩm trong bộ dữ liệu minh họa</p></div><span className="text-xs font-medium text-slate-400">Chưa kết nối Catalog API</span></div>
          {children}
        </div>
      </div>

      <dialog aria-labelledby="catalog-drawer-title" className="fixed inset-y-0 right-0 left-auto m-0 h-dvh max-h-dvh w-full max-w-sm overflow-hidden border-0 bg-white p-0 shadow-2xl backdrop:bg-slate-950/55" ref={dialogRef}>
        <div className="flex h-full flex-col">
          <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4"><div><p className="text-[11px] font-black tracking-[0.18em] text-brand uppercase">Chọn theo ý bạn</p><h2 className="mt-1 text-xl font-black text-slate-950" id="catalog-drawer-title">Bộ lọc sản phẩm</h2></div><button aria-label="Đóng bộ lọc" className="grid size-10 place-items-center rounded-xl bg-slate-100 text-slate-600" onClick={() => dialogRef.current?.close()} type="button"><X className="size-5" /></button></div>
          <div className="min-h-0 flex-1 overflow-y-auto px-5"><FacetGroups draft={draft} setDraft={setDraft} /></div>
          <div className="grid grid-cols-[1fr_2fr] gap-3 border-t border-slate-100 bg-white p-4"><button className="rounded-xl border border-slate-200 px-3 py-3 text-sm font-bold text-slate-700" onClick={clearAll} type="button">Xóa lọc</button><button className="rounded-xl bg-emerald-950 px-3 py-3 text-sm font-bold text-white" disabled={pending} onClick={() => apply()} type="button">{pending ? "Đang lọc..." : `Xem ${draftCount} sản phẩm`}</button></div>
        </div>
      </dialog>
    </>
  );
}
