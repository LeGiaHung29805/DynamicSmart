"use client";

import type { FormEvent, ReactNode } from "react";
import { useRef, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Check, ChevronDown, Search, SlidersHorizontal, X } from "lucide-react";
import type { CatalogFilterDefinition, CatalogFilters } from "../types";

export interface CategoryOption {
  id: string;
  label: string;
  slug: string;
  depth: number;
}

interface DraftFilters {
  q: string;
  category: string;
  minimumPriceVnd: string;
  maximumPriceVnd: string;
  attributes: Record<string, string>;
  sort: string;
}

function fromFilters(filters: CatalogFilters): DraftFilters {
  const attributes: Record<string, string> = {};
  filters.attribute?.forEach((entry) => {
    const separator = entry.indexOf(":");
    if (separator > 0) attributes[entry.slice(0, separator)] = entry.slice(separator + 1);
  });
  return {
    q: filters.q ?? "",
    category: filters.category ?? "",
    minimumPriceVnd: filters.minimumPriceVnd ?? "",
    maximumPriceVnd: filters.maximumPriceVnd ?? "",
    attributes,
    sort: filters.sort ?? "newest",
  };
}

function catalogUrl(draft: DraftFilters): string {
  const params = new URLSearchParams();
  if (draft.q.trim()) params.set("q", draft.q.trim());
  if (draft.category) params.set("category", draft.category);
  if (draft.minimumPriceVnd) params.set("minimumPriceVnd", draft.minimumPriceVnd);
  if (draft.maximumPriceVnd) params.set("maximumPriceVnd", draft.maximumPriceVnd);
  Object.entries(draft.attributes).forEach(([code, value]) => {
    if (value) params.append("attribute", `${code}:${value}`);
  });
  if (draft.sort !== "newest") params.set("sort", draft.sort);
  return `/products${params.size ? `?${params.toString()}` : ""}`;
}

function FacetGroup({ title, children }: Readonly<{ title: string; children: ReactNode }>) {
  return (
    <details className="group border-b border-slate-100 py-5 last:border-b-0" open>
      <summary className="flex cursor-pointer list-none items-center justify-between text-sm font-black text-slate-900 marker:hidden [&::-webkit-details-marker]:hidden">
        {title}<ChevronDown className="size-4 text-slate-400 transition group-open:rotate-180" />
      </summary>
      <div className="mt-3 space-y-1">{children}</div>
    </details>
  );
}

function Choice({ label, selected, onClick }: Readonly<{ label: string; selected: boolean; onClick: () => void }>) {
  return (
    <button aria-pressed={selected} className={`flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm transition ${selected ? "bg-emerald-50 font-bold text-emerald-900" : "text-slate-600 hover:bg-slate-50 hover:text-slate-950"}`} onClick={onClick} type="button">
      <span className={`grid size-[18px] shrink-0 place-items-center rounded-full border ${selected ? "border-brand bg-brand text-white" : "border-slate-300 bg-white"}`}>{selected ? <Check className="size-3" /> : null}</span>
      <span>{label}</span>
    </button>
  );
}

function FilterFields({ categories, definitions, draft, setDraft }: Readonly<{
  categories: CategoryOption[];
  definitions: CatalogFilterDefinition[];
  draft: DraftFilters;
  setDraft: (value: DraftFilters) => void;
}>) {
  return (
    <div>
      <FacetGroup title="Danh mục">
        <Choice label="Tất cả danh mục" onClick={() => setDraft({ ...draft, category: "", attributes: {} })} selected={!draft.category} />
        {categories.map((category) => <Choice key={category.id} label={`${"— ".repeat(category.depth)}${category.label}`} onClick={() => setDraft({ ...draft, category: category.slug, attributes: {} })} selected={draft.category === category.slug} />)}
      </FacetGroup>
      <FacetGroup title="Khoảng giá">
        <div className="grid grid-cols-2 gap-2 px-1">
          <label className="space-y-1 text-xs font-semibold text-slate-500">Từ
            <input className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-900 outline-none focus:border-brand" min="0" onChange={(event) => setDraft({ ...draft, minimumPriceVnd: event.target.value })} placeholder="0" type="number" value={draft.minimumPriceVnd} />
          </label>
          <label className="space-y-1 text-xs font-semibold text-slate-500">Đến
            <input className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-900 outline-none focus:border-brand" min="0" onChange={(event) => setDraft({ ...draft, maximumPriceVnd: event.target.value })} placeholder="Không giới hạn" type="number" value={draft.maximumPriceVnd} />
          </label>
        </div>
      </FacetGroup>
      {definitions.map((definition) => (
        <FacetGroup key={definition.attributeId} title={definition.name}>
          {definition.options.length ? <>
            <Choice label={`Tất cả ${definition.name.toLocaleLowerCase("vi-VN")}`} onClick={() => setDraft({ ...draft, attributes: { ...draft.attributes, [definition.code]: "" } })} selected={!draft.attributes[definition.code]} />
            {definition.options.map((option) => <Choice key={option.code} label={option.label} onClick={() => setDraft({ ...draft, attributes: { ...draft.attributes, [definition.code]: option.code } })} selected={draft.attributes[definition.code] === option.code} />)}
          </> : definition.dataType === "BOOLEAN" ? <>
            <Choice label="Tất cả" onClick={() => setDraft({ ...draft, attributes: { ...draft.attributes, [definition.code]: "" } })} selected={!draft.attributes[definition.code]} />
            <Choice label="Có" onClick={() => setDraft({ ...draft, attributes: { ...draft.attributes, [definition.code]: "true" } })} selected={draft.attributes[definition.code] === "true"} />
            <Choice label="Không" onClick={() => setDraft({ ...draft, attributes: { ...draft.attributes, [definition.code]: "false" } })} selected={draft.attributes[definition.code] === "false"} />
          </> : <label className="block px-1 text-xs font-semibold text-slate-500">
            Giá trị
            <input
              className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-900 outline-none focus:border-brand"
              onChange={(event) => setDraft({ ...draft, attributes: { ...draft.attributes, [definition.code]: event.target.value } })}
              step={definition.dataType === "DECIMAL" ? "any" : undefined}
              type={definition.dataType === "NUMBER" || definition.dataType === "DECIMAL" ? "number" : "text"}
              value={draft.attributes[definition.code] ?? ""}
            />
          </label>}
        </FacetGroup>
      ))}
    </div>
  );
}

export function CatalogFilterControls({ categories, definitions, filters, total, children }: Readonly<{
  categories: CategoryOption[];
  definitions: CatalogFilterDefinition[];
  filters: CatalogFilters;
  total: number;
  children: ReactNode;
}>) {
  const router = useRouter();
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [draft, setDraft] = useState(() => fromFilters(filters));
  const [pending, startTransition] = useTransition();
  const applied = fromFilters(filters);
  const activeCount = Number(Boolean(applied.q)) + Number(Boolean(applied.category))
    + Number(Boolean(applied.minimumPriceVnd || applied.maximumPriceVnd))
    + Object.values(applied.attributes).filter(Boolean).length;

  function navigate(next: DraftFilters) {
    dialogRef.current?.close();
    startTransition(() => router.push(catalogUrl(next)));
  }

  function submit(event?: FormEvent) {
    event?.preventDefault();
    navigate(draft);
  }

  function clearAll() {
    const empty = fromFilters({});
    setDraft(empty);
    navigate(empty);
  }

  const chips = [
    applied.q ? { key: "q", label: `“${applied.q}”` } : null,
    applied.category ? { key: "category", label: categories.find((item) => item.slug === applied.category)?.label ?? applied.category } : null,
    applied.minimumPriceVnd || applied.maximumPriceVnd ? { key: "price", label: "Khoảng giá" } : null,
    ...Object.entries(applied.attributes).filter(([, value]) => value).map(([code, value]) => ({ key: `attribute:${code}`, label: definitions.find((item) => item.code === code)?.options.find((item) => item.code === value)?.label ?? value })),
  ].filter((chip): chip is { key: string; label: string } => chip !== null);

  function removeChip(key: string) {
    const next = { ...applied, attributes: { ...applied.attributes } };
    if (key === "q") next.q = "";
    else if (key === "category") { next.category = ""; next.attributes = {}; }
    else if (key === "price") { next.minimumPriceVnd = ""; next.maximumPriceVnd = ""; }
    else if (key.startsWith("attribute:")) next.attributes[key.slice(10)] = "";
    setDraft(next);
    navigate(next);
  }

  return (
    <>
      <div className="grid items-start gap-5 lg:grid-cols-[280px_minmax(0,1fr)] lg:gap-7">
        <div className="min-w-0 space-y-4 lg:col-start-2 lg:row-start-1">
          <div className="flex flex-col gap-3 rounded-[1.75rem] border border-slate-200/80 bg-white p-3 shadow-sm md:flex-row md:items-center">
            <form className="flex min-w-0 flex-1 items-center gap-3 rounded-2xl bg-slate-50 px-4 py-2 focus-within:ring-2 focus-within:ring-emerald-200" onSubmit={submit}>
              <Search className="size-5 shrink-0 text-slate-400" /><label className="sr-only" htmlFor="catalog-search">Tìm sản phẩm</label>
              <input className="min-w-0 flex-1 bg-transparent py-1.5 text-sm outline-none" id="catalog-search" onChange={(event) => setDraft({ ...draft, q: event.target.value })} placeholder="Tên sản phẩm hoặc mã hàng..." value={draft.q} />
              <button className="rounded-xl bg-emerald-950 px-5 py-2 text-sm font-bold text-white" disabled={pending} type="submit">Tìm</button>
            </form>
            <div className="flex gap-2">
              <button className="inline-flex min-h-11 flex-1 items-center justify-center gap-2 rounded-xl border border-slate-200 px-4 text-sm font-bold lg:hidden" onClick={() => dialogRef.current?.showModal()} type="button"><SlidersHorizontal className="size-4" />Bộ lọc{activeCount ? ` (${activeCount})` : ""}</button>
              <select aria-label="Sắp xếp sản phẩm" className="min-h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm font-bold outline-none" onChange={(event) => navigate({ ...applied, sort: event.target.value })} value={applied.sort}>
                <option value="newest">Mới nhất</option><option value="best-selling">Bán chạy</option><option value="price-asc">Giá tăng dần</option><option value="price-desc">Giá giảm dần</option>
              </select>
            </div>
          </div>
          {chips.length ? <div className="flex flex-wrap items-center gap-2"><span className="text-xs font-semibold text-slate-500">Đang lọc:</span>{chips.map((chip) => <button className="inline-flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-xs font-bold text-emerald-900" key={chip.key} onClick={() => removeChip(chip.key)} type="button">{chip.label}<X className="size-3" /></button>)}<button className="text-xs font-bold text-slate-500" onClick={clearAll} type="button">Xóa tất cả</button></div> : null}
        </div>
        <aside className="hidden lg:col-start-1 lg:row-start-1 lg:row-span-2 lg:block">
          <div className="sticky top-24 overflow-hidden rounded-[1.75rem] border border-slate-200/80 bg-white shadow-sm">
            <div className="border-b border-slate-100 px-5 py-4"><p className="text-[11px] font-black tracking-[0.15em] text-brand uppercase">Tùy chọn mua sắm</p><h2 className="mt-1 text-lg font-black">Lọc sản phẩm</h2></div>
            <div className="filter-sidebar-scroll max-h-[calc(100vh-15rem)] overflow-y-auto px-5"><FilterFields categories={categories} definitions={definitions} draft={draft} setDraft={setDraft} /></div>
            <div className="border-t border-slate-100 p-4"><button className="w-full rounded-xl bg-emerald-950 px-4 py-3 text-sm font-bold text-white disabled:opacity-60" disabled={pending} onClick={() => submit()} type="button">{pending ? "Đang lọc..." : "Áp dụng bộ lọc"}</button></div>
          </div>
        </aside>
        <div className="min-w-0 space-y-5 lg:col-start-2 lg:row-start-2">
          <div><p className="text-xl font-black text-slate-950">Lựa chọn dành cho bạn</p><p className="mt-1 text-sm text-slate-500">{total.toLocaleString("vi-VN")} sản phẩm phù hợp</p></div>
          {children}
        </div>
      </div>
      <dialog aria-labelledby="catalog-filter-title" className="fixed inset-y-0 right-0 left-auto m-0 h-dvh max-h-dvh w-full max-w-sm overflow-hidden border-0 bg-white p-0 shadow-2xl backdrop:bg-slate-950/55" ref={dialogRef}>
        <div className="flex h-full flex-col"><div className="flex items-center justify-between border-b border-slate-100 px-5 py-4"><h2 className="text-xl font-black" id="catalog-filter-title">Bộ lọc sản phẩm</h2><button aria-label="Đóng" className="grid size-10 place-items-center rounded-xl bg-slate-100" onClick={() => dialogRef.current?.close()} type="button"><X className="size-5" /></button></div><div className="min-h-0 flex-1 overflow-y-auto px-5"><FilterFields categories={categories} definitions={definitions} draft={draft} setDraft={setDraft} /></div><div className="grid grid-cols-[1fr_2fr] gap-3 border-t border-slate-100 p-4"><button className="rounded-xl border border-slate-200 px-3 py-3 text-sm font-bold" onClick={clearAll} type="button">Xóa lọc</button><button className="rounded-xl bg-emerald-950 px-3 py-3 text-sm font-bold text-white" onClick={() => submit()} type="button">Áp dụng</button></div></div>
      </dialog>
    </>
  );
}
