"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import {
  ArrowUpRight,
  ChevronLeft,
  ChevronRight,
  Dumbbell,
  Footprints,
  Gem,
  Gift,
  Headphones,
  House,
  Shirt,
  ShoppingBag,
  Sparkles,
  Watch,
  type LucideIcon,
} from "lucide-react";
import { Button } from "@/components/ui/Button";
import { brands, categories, heroSlides } from "../data";

const categoryIcons: Record<string, LucideIcon> = {
  shirt: Shirt,
  sparkles: Sparkles,
  watch: Watch,
  footprints: Footprints,
  shoppingBag: ShoppingBag,
  gift: Gift,
  gem: Gem,
  headphones: Headphones,
  house: House,
  dumbbell: Dumbbell,
};

function useAutoplay(total: number, delay: number) {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);

  useEffect(() => {
    if (paused || total < 2) return;
    const timer = window.setInterval(() => setIndex((current) => (current + 1) % total), delay);
    return () => window.clearInterval(timer);
  }, [delay, paused, total]);

  return {
    index,
    setIndex,
    setPaused,
    previous: () => setIndex((current) => (current - 1 + total) % total),
    next: () => setIndex((current) => (current + 1) % total),
  };
}

function useItemsPerPage() {
  const [itemsPerPage, setItemsPerPage] = useState(2);

  useEffect(() => {
    const update = () => {
      if (window.innerWidth >= 1024) setItemsPerPage(5);
      else if (window.innerWidth >= 640) setItemsPerPage(3);
      else setItemsPerPage(2);
    };
    update();
    window.addEventListener("resize", update);
    return () => window.removeEventListener("resize", update);
  }, []);

  return itemsPerPage;
}

function CarouselControls({
  current,
  total,
  label,
  onPrevious,
  onNext,
}: Readonly<{
  current: number;
  total: number;
  label: string;
  onPrevious: () => void;
  onNext: () => void;
}>) {
  return (
    <div className="flex items-center gap-2">
      <span className="mr-1 hidden text-xs font-semibold tabular-nums text-slate-400 sm:inline">
        {String(current + 1).padStart(2, "0")} / {String(total).padStart(2, "0")}
      </span>
      <Button aria-label={`Trang trước của ${label}`} onClick={onPrevious} size="icon-lg" type="button" variant="outline">
        <ChevronLeft />
      </Button>
      <Button aria-label={`Trang sau của ${label}`} onClick={onNext} size="icon-lg" type="button" variant="outline">
        <ChevronRight />
      </Button>
    </div>
  );
}

export function HeroCarousel() {
  const { index, next, previous, setIndex, setPaused } = useAutoplay(heroSlides.length, 6500);
  const slide = heroSlides[index];

  return (
    <section
      aria-label="Ưu đãi nổi bật"
      className="relative isolate min-h-[620px] overflow-hidden bg-slate-950 text-white sm:min-h-[680px]"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
    >
      <div className="absolute inset-0" key={slide.id}>
        <Image
          alt=""
          className="animate-in fade-in object-cover duration-700"
          fill
          priority={index === 0}
          sizes="100vw"
          src={slide.image}
        />
        <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(2,6,23,.96)_0%,rgba(2,6,23,.82)_38%,rgba(2,6,23,.22)_72%,rgba(2,6,23,.46)_100%)]" />
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_72%_45%,transparent_0%,rgba(2,6,23,.18)_48%,rgba(2,6,23,.58)_100%)]" />
      </div>

      <div className="relative mx-auto flex min-h-[620px] w-full max-w-7xl items-center px-4 py-20 sm:min-h-[680px] sm:px-6 lg:px-8">
        <div className="max-w-2xl animate-in slide-in-from-left-5 fade-in duration-700" key={`${slide.id}-content`}>
          <div className="mb-6 flex items-center gap-3">
            <span className="h-px w-9 bg-current opacity-70" />
            <p className={`text-xs font-black tracking-[0.24em] uppercase sm:text-sm ${slide.accent}`}>{slide.eyebrow}</p>
          </div>
          <h1 className="max-w-2xl text-5xl leading-[1.04] font-black tracking-[-0.045em] text-balance sm:text-6xl lg:text-7xl">
            {slide.title}
          </h1>
          <p className="mt-6 max-w-xl text-base leading-7 text-slate-200 sm:text-lg sm:leading-8">{slide.description}</p>
          <div className="mt-9 flex flex-wrap gap-3">
            <Link className="group inline-flex min-h-12 items-center gap-2 rounded-xl bg-white px-5 py-3 text-sm font-bold text-slate-950 shadow-xl shadow-black/20 transition hover:-translate-y-0.5 hover:bg-emerald-50" href={slide.primaryHref}>
              {slide.primaryLabel}<ArrowUpRight className="size-4 transition group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
            </Link>
            <Link className="inline-flex min-h-12 items-center rounded-xl border border-white/25 bg-white/5 px-5 py-3 text-sm font-bold text-white backdrop-blur transition hover:border-white/60 hover:bg-white/10" href={slide.secondaryHref}>
              {slide.secondaryLabel}
            </Link>
          </div>
          <div className="mt-12 flex flex-wrap gap-x-8 gap-y-4 border-t border-white/15 pt-7 text-sm text-slate-300">
            <p><strong className="mr-2 text-xl text-white">10K+</strong>Sản phẩm chọn lọc</p>
            <p><strong className="mr-2 text-xl text-white">100%</strong>Chính hãng</p>
            <p><strong className="mr-2 text-xl text-white">7 ngày</strong>Đổi trả</p>
          </div>
        </div>
      </div>

      <div className="absolute right-4 bottom-5 left-4 mx-auto flex max-w-7xl items-center justify-between sm:right-6 sm:bottom-7 sm:left-6 lg:right-8 lg:left-8">
        <div className="flex items-center gap-2">
          {heroSlides.map((item, itemIndex) => (
            <button
              aria-label={`Đến banner ${itemIndex + 1}`}
              className={`h-1.5 rounded-full transition-all duration-500 ${itemIndex === index ? "w-10 bg-white" : "w-4 bg-white/35 hover:bg-white/60"}`}
              key={item.id}
              onClick={() => setIndex(itemIndex)}
              type="button"
            />
          ))}
        </div>
        <div className="flex gap-2">
          <Button aria-label="Banner trước" className="border-white/20 bg-black/20 text-white hover:bg-white hover:text-slate-950" onClick={previous} size="icon-lg" type="button" variant="outline"><ChevronLeft /></Button>
          <Button aria-label="Banner tiếp theo" className="border-white/20 bg-black/20 text-white hover:bg-white hover:text-slate-950" onClick={next} size="icon-lg" type="button" variant="outline"><ChevronRight /></Button>
        </div>
      </div>
    </section>
  );
}

export function CategoryCarousel() {
  const itemsPerPage = useItemsPerPage();
  const total = Math.ceil(categories.length / itemsPerPage);
  const { index, next, previous, setIndex, setPaused } = useAutoplay(total, 5200);
  const safeIndex = Math.min(index, total - 1);
  const visibleItems = useMemo(
    () => categories.slice(safeIndex * itemsPerPage, safeIndex * itemsPerPage + itemsPerPage),
    [itemsPerPage, safeIndex],
  );

  useEffect(() => setIndex(0), [itemsPerPage, setIndex]);

  return (
    <div onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)}>
      <div className="mb-6 flex items-end justify-between gap-4">
        <div>
          <p className="mb-2 text-xs font-black tracking-[0.2em] text-brand uppercase">Chọn theo phong cách</p>
          <h2 className="text-3xl font-black tracking-[-0.035em] text-slate-950 sm:text-4xl">Danh mục nổi bật</h2>
        </div>
        <CarouselControls current={safeIndex} label="danh mục" onNext={next} onPrevious={previous} total={total} />
      </div>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5" key={`${itemsPerPage}-${safeIndex}`}>
        {visibleItems.map((category, itemIndex) => {
          const Icon = categoryIcons[category.icon];
          return (
            <Link
              className="group animate-in fade-in slide-in-from-right-3 rounded-3xl border border-slate-200/80 bg-white p-4 shadow-sm transition duration-300 hover:-translate-y-1.5 hover:border-emerald-200 hover:shadow-xl hover:shadow-emerald-950/5 sm:p-5"
              href={category.href}
              key={category.name}
              style={{ animationDelay: `${itemIndex * 65}ms` }}
            >
              <span className={`grid aspect-square place-items-center rounded-2xl bg-gradient-to-br ${category.color}`}>
                <Icon className="size-8 transition duration-300 group-hover:scale-110 sm:size-10" strokeWidth={1.65} />
              </span>
              <div className="mt-4 flex items-start justify-between gap-2">
                <div><h3 className="font-bold text-slate-900">{category.name}</h3><p className="mt-1 text-xs text-slate-500">{category.caption}</p></div>
                <ArrowUpRight className="mt-0.5 size-4 shrink-0 text-slate-300 transition group-hover:text-brand" />
              </div>
            </Link>
          );
        })}
      </div>
      <div className="mt-5 flex justify-center gap-1.5">
        {Array.from({ length: total }, (_, page) => <button aria-label={`Trang danh mục ${page + 1}`} className={`h-1.5 rounded-full transition-all ${page === safeIndex ? "w-8 bg-brand" : "w-2 bg-slate-200 hover:bg-slate-300"}`} key={page} onClick={() => setIndex(page)} type="button" />)}
      </div>
    </div>
  );
}

export function BrandCarousel() {
  const itemsPerPage = useItemsPerPage();
  const total = Math.ceil(brands.length / itemsPerPage);
  const { index, next, previous, setIndex, setPaused } = useAutoplay(total, 4700);
  const safeIndex = Math.min(index, total - 1);
  const visibleItems = useMemo(
    () => brands.slice(safeIndex * itemsPerPage, safeIndex * itemsPerPage + itemsPerPage),
    [itemsPerPage, safeIndex],
  );

  useEffect(() => setIndex(0), [itemsPerPage, setIndex]);

  return (
    <div id="brands" onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)}>
      <div className="mb-6 flex items-end justify-between gap-4">
        <div>
          <p className="mb-2 text-xs font-black tracking-[0.2em] text-amber-700 uppercase">Hàng thật · Giá thật</p>
          <h2 className="text-3xl font-black tracking-[-0.035em] text-slate-950 sm:text-4xl">Thương hiệu được yêu thích</h2>
        </div>
        <CarouselControls current={safeIndex} label="thương hiệu" onNext={next} onPrevious={previous} total={total} />
      </div>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5" key={`${itemsPerPage}-${safeIndex}`}>
        {visibleItems.map((brand, itemIndex) => (
          <Link
            className={`group relative isolate flex aspect-[4/3] animate-in flex-col justify-between overflow-hidden rounded-3xl bg-gradient-to-br p-5 shadow-sm ring-1 ring-black/5 transition duration-300 fade-in slide-in-from-right-3 hover:-translate-y-1.5 hover:shadow-xl ${brand.color}`}
            href={`/products?brand=${brand.name.toLowerCase()}`}
            key={brand.name}
            style={{ animationDelay: `${itemIndex * 65}ms` }}
          >
            <span className="absolute -right-3 -bottom-9 -z-10 text-[8rem] leading-none font-black opacity-[0.08] transition duration-500 group-hover:-translate-y-2 group-hover:scale-105">{brand.mark}</span>
            <span className="text-xl font-black tracking-[-0.04em] sm:text-2xl">{brand.name}</span>
            <span className="flex items-end justify-between gap-2 text-[11px] font-semibold opacity-75 sm:text-xs">{brand.subtitle}<ArrowUpRight className="size-4 transition group-hover:translate-x-0.5 group-hover:-translate-y-0.5" /></span>
          </Link>
        ))}
      </div>
      <div className="mt-5 flex justify-center gap-1.5">
        {Array.from({ length: total }, (_, page) => <button aria-label={`Trang thương hiệu ${page + 1}`} className={`h-1.5 rounded-full transition-all ${page === safeIndex ? "w-8 bg-amber-500" : "w-2 bg-amber-200 hover:bg-amber-300"}`} key={page} onClick={() => setIndex(page)} type="button" />)}
      </div>
    </div>
  );
}
