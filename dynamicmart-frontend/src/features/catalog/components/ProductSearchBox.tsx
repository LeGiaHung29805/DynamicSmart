"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import type { FormEvent, KeyboardEvent as ReactKeyboardEvent } from "react";
import { useEffect, useId, useMemo, useRef, useState } from "react";
import { ArrowRight, FolderTree, LoaderCircle, PackageSearch, Search } from "lucide-react";
import { getProductSearchSuggestions } from "../api/product-search.api";
import type { ProductSearchSuggestions } from "../types";

const EMPTY_SUGGESTIONS: ProductSearchSuggestions = { products: [], categories: [] };

interface SearchItem {
  key: string;
  label: string;
  description: string;
  href: string;
  imageUrl?: string | null;
  price?: number | null;
  type: "product" | "category";
}

function productPrice(value: number | null | undefined) {
  return value == null ? "" : new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value);
}

export function ProductSearchBox({
  className = "",
  placeholder = "Tìm sản phẩm hoặc danh mục...",
}: Readonly<{
  className?: string;
  placeholder?: string;
}>) {
  const router = useRouter();
  const listboxId = useId();
  const rootRef = useRef<HTMLDivElement>(null);
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState(EMPTY_SUGGESTIONS);
  const [status, setStatus] = useState<"idle" | "loading" | "success" | "error">("idle");
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);

  const items = useMemo<SearchItem[]>(() => [
    ...suggestions.products.map((product) => ({
      key: `product-${product.id}`,
      label: product.name,
      description: product.categoryName,
      href: `/products/${encodeURIComponent(product.slug)}`,
      imageUrl: product.imageUrl,
      price: product.minimumPriceVnd,
      type: "product" as const,
    })),
    ...suggestions.categories.map((category) => ({
      key: `category-${category.id}`,
      label: category.name,
      description: "Danh mục sản phẩm",
      href: `/products?category=${encodeURIComponent(category.slug)}`,
      type: "category" as const,
    })),
  ], [suggestions]);

  useEffect(() => {
    const safeQuery = query.trim();
    if (safeQuery.length < 2) return;

    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      void getProductSearchSuggestions(safeQuery, controller.signal)
        .then((result) => {
          if (controller.signal.aborted) return;
          setSuggestions(result);
          setStatus("success");
          setActiveIndex(-1);
        })
        .catch(() => {
          if (controller.signal.aborted) return;
          setSuggestions(EMPTY_SUGGESTIONS);
          setStatus("error");
          setActiveIndex(-1);
        });
    }, 250);

    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [query]);

  useEffect(() => {
    function closeWhenClickingOutside(event: PointerEvent) {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false);
    }
    document.addEventListener("pointerdown", closeWhenClickingOutside);
    return () => document.removeEventListener("pointerdown", closeWhenClickingOutside);
  }, []);

  function navigate(href: string) {
    setOpen(false);
    setActiveIndex(-1);
    router.push(href);
  }

  function updateQuery(value: string) {
    setQuery(value);
    setActiveIndex(-1);
    if (value.trim().length < 2) {
      setSuggestions(EMPTY_SUGGESTIONS);
      setStatus("idle");
      setOpen(false);
    } else {
      setSuggestions(EMPTY_SUGGESTIONS);
      setStatus("loading");
      setOpen(true);
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const safeQuery = query.trim();
    if (!safeQuery) return;
    navigate(`/products?q=${encodeURIComponent(safeQuery)}`);
  }

  function handleKeyboard(event: ReactKeyboardEvent<HTMLInputElement>) {
    if (event.key === "Escape") {
      setOpen(false);
      setActiveIndex(-1);
      return;
    }
    if (event.key === "ArrowDown" && items.length) {
      event.preventDefault();
      setOpen(true);
      setActiveIndex((current) => (current + 1) % items.length);
      return;
    }
    if (event.key === "ArrowUp" && items.length) {
      event.preventDefault();
      setOpen(true);
      setActiveIndex((current) => (current <= 0 ? items.length - 1 : current - 1));
      return;
    }
    if (event.key === "Enter" && open && activeIndex >= 0 && items[activeIndex]) {
      event.preventDefault();
      navigate(items[activeIndex].href);
    }
  }

  function optionId(item: SearchItem) {
    return `${listboxId}-${item.key}`;
  }

  function renderItem(item: SearchItem) {
    const index = items.findIndex((candidate) => candidate.key === item.key);
    const active = index === activeIndex;
    return (
      <Link
        aria-selected={active}
        className={`flex items-center gap-3 rounded-xl px-3 py-2.5 transition ${active ? "bg-emerald-50" : "hover:bg-slate-50"}`}
        href={item.href}
        id={optionId(item)}
        key={item.key}
        onClick={() => setOpen(false)}
        onMouseEnter={() => setActiveIndex(index)}
        role="option"
      >
        {item.type === "product" ? (
          item.imageUrl ? (
            <span
              aria-label={`Ảnh ${item.label}`}
              className="size-11 shrink-0 rounded-xl border border-slate-100 bg-slate-100 bg-cover bg-center"
              role="img"
              style={{ backgroundImage: `url(${JSON.stringify(item.imageUrl)})` }}
            />
          ) : <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-slate-100 text-slate-500"><PackageSearch className="size-5" /></span>
        ) : <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-amber-50 text-amber-700"><FolderTree className="size-5" /></span>}
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-bold text-slate-900">{item.label}</span>
          <span className="block truncate text-xs text-slate-500">{item.description}</span>
        </span>
        {item.price != null ? <span className="shrink-0 text-xs font-black text-brand">{productPrice(item.price)}</span> : <ArrowRight className="size-4 shrink-0 text-slate-400" />}
      </Link>
    );
  }

  const showPanel = open && query.trim().length >= 2;
  const hasResults = items.length > 0;

  return (
    <div className={`relative min-w-0 ${className}`} ref={rootRef}>
      <form
        className="group flex min-w-0 items-center rounded-xl border border-slate-200 bg-slate-50 p-1 transition focus-within:border-emerald-500 focus-within:bg-white focus-within:ring-4 focus-within:ring-emerald-500/10"
        onSubmit={submit}
        role="search"
      >
        <Search className="ml-2 size-4 shrink-0 text-slate-400 transition group-focus-within:text-brand" />
        <input
          aria-activedescendant={activeIndex >= 0 && items[activeIndex] ? optionId(items[activeIndex]) : undefined}
          aria-autocomplete="list"
          aria-controls={listboxId}
          aria-expanded={showPanel}
          aria-label="Tìm kiếm sản phẩm"
          autoComplete="off"
          className="min-w-0 flex-1 bg-transparent px-3 py-1 text-sm text-slate-900 outline-none placeholder:text-slate-400"
          onChange={(event) => updateQuery(event.target.value)}
          onFocus={() => { if (query.trim().length >= 2) setOpen(true); }}
          onKeyDown={handleKeyboard}
          placeholder={placeholder}
          role="combobox"
          type="search"
          value={query}
        />
        <button className="shrink-0 rounded-lg bg-slate-950 px-3 py-1.5 text-xs font-bold text-white transition hover:bg-brand" type="submit">
          Tìm
        </button>
      </form>

      {showPanel ? (
        <div className="absolute top-[calc(100%+0.5rem)] right-0 left-0 z-[70] max-h-[min(32rem,70vh)] overflow-y-auto rounded-2xl border border-slate-200 bg-white p-2 shadow-2xl shadow-slate-950/15">
          {status === "loading" && !hasResults ? <div className="flex items-center justify-center gap-2 px-4 py-8 text-sm text-slate-500" role="status"><LoaderCircle className="size-4 animate-spin" />Đang tìm gợi ý...</div> : null}
          {status === "error" ? <div className="px-4 py-6 text-center text-sm text-slate-500">Không tải được gợi ý. Bạn vẫn có thể nhấn Enter để tìm kiếm.</div> : null}
          {status === "success" && !hasResults ? <div className="px-4 py-6 text-center text-sm text-slate-500">Không có gợi ý phù hợp. Nhấn Enter để xem kết quả đầy đủ.</div> : null}
          {hasResults ? <>
            <div id={listboxId} role="listbox">
              {suggestions.products.length ? <section aria-label="Sản phẩm gợi ý" role="group"><p className="px-3 pt-2 pb-1 text-[0.68rem] font-black tracking-[0.14em] text-slate-400 uppercase">Sản phẩm</p>{suggestions.products.map((product) => renderItem(items.find((item) => item.key === `product-${product.id}`)!))}</section> : null}
              {suggestions.categories.length ? <section aria-label="Danh mục gợi ý" role="group"><p className="px-3 pt-3 pb-1 text-[0.68rem] font-black tracking-[0.14em] text-slate-400 uppercase">Danh mục</p>{suggestions.categories.map((category) => renderItem(items.find((item) => item.key === `category-${category.id}`)!))}</section> : null}
            </div>
            <button className="mt-1 flex w-full items-center justify-between rounded-xl border-t border-slate-100 px-3 py-3 text-left text-sm font-bold text-brand hover:bg-emerald-50" onClick={() => navigate(`/products?q=${encodeURIComponent(query.trim())}`)} type="button"><span>Xem tất cả kết quả cho “{query.trim()}”</span><ArrowRight className="size-4" /></button>
          </> : null}
        </div>
      ) : null}
    </div>
  );
}
