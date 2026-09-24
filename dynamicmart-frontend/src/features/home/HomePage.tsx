import Image from "next/image";
import Link from "next/link";
import { ArrowRight, Search, Sparkles } from "lucide-react";
import { CatalogProductCard } from "@/features/catalog/components/CatalogProductCard";
import { getCategories, getProducts } from "@/features/catalog/api/catalog.api";
import type { CategoryTree, ProductSummary } from "@/features/catalog/types";
import { BrandCarousel, CategoryCarousel, HeroCarousel, type HomeCategoryItem } from "./components/HomeCarousels";

function SectionTitle({
  eyebrow,
  title,
  description,
  href,
  action = "Xem tất cả",
}: Readonly<{ eyebrow: string; title: string; description?: string; href?: string; action?: string }>) {
  return (
    <div className="mb-7 flex items-end justify-between gap-5">
      <div>
        <p className="mb-2 text-xs font-black tracking-[0.2em] text-brand uppercase">{eyebrow}</p>
        <h2 className="text-3xl font-black tracking-[-0.035em] text-slate-950 sm:text-4xl">{title}</h2>
        {description ? <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-500 sm:text-base">{description}</p> : null}
      </div>
      {href ? (
        <Link className="group hidden shrink-0 items-center gap-2 text-sm font-bold text-slate-700 transition hover:text-brand sm:flex" href={href}>
          {action}<ArrowRight className="size-4 transition group-hover:translate-x-1" />
        </Link>
      ) : null}
    </div>
  );
}

// const benefits = [
//   { icon: Truck, title: "Miễn phí vận chuyển", detail: "Cho đơn hàng từ 499K" },
//   { icon: ShieldCheck, title: "Cam kết chính hãng", detail: "Hoàn tiền nếu hàng giả" },
//   { icon: RefreshCcw, title: "Đổi trả linh hoạt", detail: "Trong vòng 7 ngày" },
//   { icon: BadgeCheck, title: "Thanh toán an toàn", detail: "Bảo mật mọi giao dịch" },
// ];

const categoryPresentation: Pick<HomeCategoryItem, "icon" | "color">[] = [
  { icon: "shirt", color: "from-sky-100 to-blue-50 text-sky-700" },
  { icon: "sparkles", color: "from-rose-100 to-pink-50 text-rose-700" },
  { icon: "watch", color: "from-amber-100 to-orange-50 text-amber-700" },
  { icon: "footprints", color: "from-violet-100 to-purple-50 text-violet-700" },
  { icon: "shoppingBag", color: "from-emerald-100 to-teal-50 text-emerald-700" },
  { icon: "gift", color: "from-red-100 to-orange-50 text-red-700" },
  { icon: "gem", color: "from-fuchsia-100 to-pink-50 text-fuchsia-700" },
  { icon: "headphones", color: "from-cyan-100 to-sky-50 text-cyan-700" },
  { icon: "house", color: "from-lime-100 to-green-50 text-lime-700" },
  { icon: "dumbbell", color: "from-indigo-100 to-blue-50 text-indigo-700" },
];

function flattenCategories(categories: CategoryTree[]): CategoryTree[] {
  return categories.flatMap((category) => [category, ...flattenCategories(category.children)]);
}

function presentCategories(categories: CategoryTree[]): HomeCategoryItem[] {
  return flattenCategories(categories).slice(0, 10).map((category, index) => ({
    ...categoryPresentation[index % categoryPresentation.length],
    name: category.name,
    caption: category.description?.trim() || "Khám phá sản phẩm",
    href: `/products?category=${encodeURIComponent(category.slug)}`,
  }));
}

function CatalogUnavailable() {
  return (
    <div className="rounded-3xl border border-amber-200 bg-amber-50 px-6 py-8 text-center">
      <p className="font-black text-amber-950">Catalog đang tạm thời chưa sẵn sàng</p>
      <p className="mt-2 text-sm text-amber-800">Hãy khởi động Gateway và catalog-service rồi tải lại trang.</p>
    </div>
  );
}

async function loadHomeCatalog(): Promise<{
  categories: HomeCategoryItem[];
  featured: ProductSummary[];
  bestSellers: ProductSummary[];
  newArrivals: ProductSummary[];
} | null> {
  try {
    const [categories, featured, bestSellers, newArrivals] = await Promise.all([
      getCategories(),
      getProducts({ featured: true, sort: "NEWEST", page: 0, size: 4 }),
      getProducts({ sort: "BEST_SELLER", page: 0, size: 4 }),
      getProducts({ sort: "NEWEST", page: 0, size: 4 }),
    ]);
    return {
      categories: presentCategories(categories),
      featured: featured.content,
      bestSellers: bestSellers.content,
      newArrivals: newArrivals.content,
    };
  } catch {
    return null;
  }
}

export async function HomePage() {
  const catalog = await loadHomeCatalog();
  return (
    <div className="overflow-hidden bg-[#f7f8f6]">
      <div className="border-b border-emerald-900/20 bg-emerald-950 text-emerald-50">
        <div className="mx-auto flex min-h-9 max-w-7xl items-center justify-center px-4 text-center text-[11px] font-semibold tracking-wide sm:justify-between sm:px-6 lg:px-8">
          <span>Miễn phí vận chuyển cho đơn từ 499.000₫</span>
          <span className="hidden sm:block">Hàng chính hãng · Đổi trả 7 ngày · Hỗ trợ mỗi ngày</span>
        </div>
      </div>

      <section className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl items-center gap-3 px-4 py-3 sm:px-6 lg:px-8">
          <p className="hidden shrink-0 text-xs font-bold tracking-wide text-slate-500 uppercase lg:block">Tìm nhanh sản phẩm bạn yêu thích</p>
          <form action="/products" className="group flex min-w-0 flex-1 items-center rounded-2xl border border-slate-200 bg-slate-50 p-1.5 transition focus-within:border-emerald-500 focus-within:bg-white focus-within:ring-4 focus-within:ring-emerald-500/10">
            <Search className="ml-2.5 size-4 shrink-0 text-slate-400 transition group-focus-within:text-brand" />
            <input aria-label="Tìm sản phẩm" className="min-w-0 flex-1 bg-transparent px-3 py-1.5 text-sm outline-none placeholder:text-slate-400" name="q" placeholder="Tìm sản phẩm, thương hiệu và danh mục..." />
            <button className="rounded-xl bg-slate-950 px-4 py-2 text-sm font-bold text-white transition hover:bg-brand" type="submit">Tìm kiếm</button>
          </form>
          <Link className="hidden shrink-0 rounded-xl px-3 py-2 text-xs font-bold text-slate-600 transition hover:bg-slate-100 hover:text-brand lg:block" href="/customer/account/vouchers">Kho voucher</Link>
        </div>
      </section>

      <HeroCarousel />

      {/* <div className="relative z-10 mx-auto -mt-px max-w-7xl px-4 sm:px-6 lg:-mt-10 lg:px-8">
        <div className="grid grid-cols-2 overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-xl shadow-slate-950/5 lg:grid-cols-4">
          {benefits.map(({ icon: Icon, title, detail }, index) => (
            <div className={`flex items-center gap-3 p-4 sm:p-5 ${index % 2 ? "border-l border-slate-100" : ""} ${index > 1 ? "border-t border-slate-100 lg:border-t-0" : ""} ${index === 2 ? "lg:border-l" : ""}`} key={title}>
              <span className="grid size-10 shrink-0 place-items-center rounded-2xl bg-emerald-50 text-brand sm:size-11"><Icon className="size-5" strokeWidth={1.8} /></span>
              <div><p className="text-xs font-bold text-slate-900 sm:text-sm">{title}</p><p className="mt-0.5 hidden text-xs text-slate-500 sm:block">{detail}</p></div>
            </div>
          ))}
        </div>
      </div> */}

      <div className="mx-auto flex w-full max-w-7xl flex-col gap-20 px-4 py-16 sm:px-6 lg:gap-28 lg:px-8 lg:py-24">
        <section>{catalog?.categories.length ? <CategoryCarousel items={catalog.categories} /> : <CatalogUnavailable />}</section>

        <section>
          <SectionTitle description="Tuyển chọn nổi bật do đội ngũ DynamicMart đề xuất cho mùa này." eyebrow="Đề xuất hôm nay" href="/products" title="Sản phẩm nổi bật" />
          {catalog?.featured.length ? <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-5 lg:grid-cols-4">{catalog.featured.map((product) => <CatalogProductCard key={product.id} product={product} />)}</div> : <CatalogUnavailable />}
        </section>

        <section className="grid gap-4 lg:grid-cols-[1.35fr_.65fr]">
          <Link className="group relative min-h-80 overflow-hidden rounded-[2rem] bg-slate-950 p-7 text-white shadow-sm sm:p-10" href="/products?category=thoi-trang-nu">
            <Image alt="Bộ sưu tập thời trang mới" className="object-cover opacity-65 transition duration-700 group-hover:scale-105" fill sizes="(max-width: 1024px) 100vw, 68vw" src="https://images.unsplash.com/photo-1469334031218-e382a71b716b?auto=format&fit=crop&w=1400&q=84" />
            <div className="absolute inset-0 bg-gradient-to-r from-slate-950 via-slate-950/65 to-transparent" />
            <div className="relative flex h-full max-w-md flex-col justify-end">
              <p className="text-xs font-black tracking-[0.2em] text-emerald-300 uppercase">Women&apos;s edit</p>
              <h2 className="mt-3 text-3xl font-black tracking-tight sm:text-4xl">Thanh lịch theo cách của riêng bạn.</h2>
              <span className="mt-6 inline-flex items-center gap-2 text-sm font-bold">Khám phá ngay <ArrowRight className="size-4 transition group-hover:translate-x-1" /></span>
            </div>
          </Link>
          <Link className="group relative min-h-80 overflow-hidden rounded-[2rem] bg-amber-300 p-7 text-slate-950 shadow-sm sm:p-10" href="/products?promotion=true">
            <div className="absolute -top-20 -right-16 size-64 rounded-full bg-white/30 blur-2xl" />
            <div className="relative flex h-full flex-col justify-between">
              <span className="grid size-12 place-items-center rounded-2xl bg-white/60"><Sparkles className="size-6" /></span>
              <div><p className="text-xs font-black tracking-[0.2em] uppercase">Deal mỗi ngày</p><h2 className="mt-3 text-4xl font-black tracking-[-0.04em]">Giảm đến<br />50%</h2><span className="mt-6 inline-flex items-center gap-2 text-sm font-bold">Săn deal ngay <ArrowRight className="size-4 transition group-hover:translate-x-1" /></span></div>
            </div>
          </Link>
        </section>

        <section className="rounded-[2rem] bg-[#f2e9d7] p-5 sm:p-8 lg:p-10"><BrandCarousel /></section>

        <section id="best-seller">
          <SectionTitle description="Những lựa chọn được cộng đồng DynamicMart yêu thích nhất trong tuần." eyebrow="Được săn đón" href="/products?sort=best-selling" title="Best seller tuần này" />
          {catalog?.bestSellers.length ? <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-5 lg:grid-cols-4">{catalog.bestSellers.map((product) => <CatalogProductCard key={product.id} product={product} />)}</div> : <CatalogUnavailable />}
          <Link className="mt-7 flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-5 py-3 text-sm font-bold text-slate-800 sm:hidden" href="/products?sort=best-selling">Xem tất cả <ArrowRight className="size-4" /></Link>
        </section>

        <section className="relative isolate grid overflow-hidden rounded-[2rem] bg-emerald-950 text-white shadow-xl shadow-emerald-950/10 lg:grid-cols-[1.05fr_.95fr]">
          <div className="relative z-10 p-7 sm:p-10 lg:p-14">
            <p className="text-xs font-black tracking-[0.2em] text-emerald-300 uppercase">Dành riêng cho bạn</p>
            <h2 className="mt-4 max-w-lg text-4xl font-black tracking-[-0.04em] sm:text-5xl">Ưu đãi 15% cho đơn hàng đầu tiên.</h2>
            <p className="mt-5 max-w-md leading-7 text-emerald-100/80">Trở thành thành viên DynamicMart để nhận voucher cá nhân, tích điểm và theo dõi đơn hàng dễ dàng.</p>
            <Link className="mt-8 inline-flex items-center gap-2 rounded-xl bg-white px-5 py-3 text-sm font-bold text-emerald-950 transition hover:-translate-y-0.5 hover:bg-emerald-50" href="/register">Đăng ký miễn phí <ArrowRight className="size-4" /></Link>
          </div>
          <div className="relative min-h-72 lg:min-h-full"><Image alt="Ưu đãi thành viên DynamicMart" className="object-cover" fill sizes="(max-width: 1024px) 100vw, 45vw" src="https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=1200&q=84" /><div className="absolute inset-0 bg-gradient-to-t from-emerald-950/55 to-transparent lg:bg-gradient-to-r" /></div>
        </section>

        <section>
          <SectionTitle description="Những thiết kế vừa cập bến — số lượng giới hạn cho mùa mới." eyebrow="Vừa lên kệ" href="/products?sort=newest" title="Sản phẩm mới về" />
          {catalog?.newArrivals.length ? <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-5 lg:grid-cols-4">{catalog.newArrivals.map((product) => <CatalogProductCard key={product.id} product={product} />)}</div> : <CatalogUnavailable />}
        </section>
      </div>
    </div>
  );
}
