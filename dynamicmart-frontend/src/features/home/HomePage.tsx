import Image from "next/image";
import Link from "next/link";
import { ArrowRight, Sparkles } from "lucide-react";
import { CatalogProductCard } from "@/features/catalog/components/CatalogProductCard";
import { getCategories, getProducts } from "@/features/catalog/api/catalog.api";
import type { CategoryTree, ProductSummary } from "@/features/catalog/types";
import { CategoryCarousel, HeroCarousel, type HomeCategoryItem } from "./components/HomeCarousels";

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

const categoryPresentation = [
  { tokens: ["electronics", "dien-tu"], icon: "cpu", color: "from-cyan-100 to-sky-50 text-cyan-700" },
  { tokens: ["fashion", "thoi-trang"], icon: "shirt", color: "from-sky-100 to-blue-50 text-sky-700" },
  { tokens: ["home_living", "nha-cua"], icon: "house", color: "from-lime-100 to-green-50 text-lime-700" },
  { tokens: ["beauty", "lam-dep"], icon: "sparkles", color: "from-rose-100 to-pink-50 text-rose-700" },
  { tokens: ["sport", "the-thao"], icon: "dumbbell", color: "from-indigo-100 to-blue-50 text-indigo-700" },
  { tokens: ["book", "sach-van-phong"], icon: "bookOpen", color: "from-amber-100 to-orange-50 text-amber-700" },
  { tokens: ["kitchen", "do-bep"], icon: "house", color: "from-orange-100 to-amber-50 text-orange-700" },
  { tokens: ["kids", "do-choi"], icon: "gift", color: "from-fuchsia-100 to-pink-50 text-fuchsia-700" },
  { tokens: ["pet", "thu-cung"], icon: "shoppingBag", color: "from-violet-100 to-purple-50 text-violet-700" },
  { tokens: ["travel", "du-lich"], icon: "footprints", color: "from-teal-100 to-cyan-50 text-teal-700" },
] satisfies ReadonlyArray<Pick<HomeCategoryItem, "icon" | "color"> & { tokens: readonly string[] }>;

const defaultCategoryPresentation: Pick<HomeCategoryItem, "icon" | "color"> = {
  icon: "shoppingBag",
  color: "from-emerald-100 to-teal-50 text-emerald-700",
};

function categoryPresentationIndex(category: CategoryTree): number {
  const searchable = `${category.code} ${category.slug}`.toLowerCase();
  return categoryPresentation.findIndex(({ tokens }) => tokens.some((token) => searchable.includes(token)));
}

function presentCategories(categories: CategoryTree[]): HomeCategoryItem[] {
  const seenGroups = new Set<string>();

  return categories
    .filter((category) => {
      const presentationIndex = categoryPresentationIndex(category);
      const groupKey = presentationIndex >= 0 ? `known-${presentationIndex}` : category.id;
      if (seenGroups.has(groupKey)) return false;
      seenGroups.add(groupKey);
      return true;
    })
    .slice(0, 16)
    .map((category) => {
      const presentationIndex = categoryPresentationIndex(category);
      const presentation = presentationIndex >= 0
        ? categoryPresentation[presentationIndex]
        : defaultCategoryPresentation;
      const childSummary = category.children.slice(0, 3).map((child) => child.name).join(" · ");

      return {
        icon: presentation.icon,
        color: presentation.color,
        name: category.name,
        caption: childSummary || category.description?.trim() || "Khám phá sản phẩm",
        href: `/products?category=${encodeURIComponent(category.slug)}`,
      };
    });
}

function EmptyCatalogSection({
  title,
  description,
}: Readonly<{ title: string; description: string }>) {
  return (
    <div className="rounded-3xl border border-amber-200 bg-amber-50 px-6 py-8 text-center">
      <p className="font-black text-amber-950">{title}</p>
      <p className="mt-2 text-sm text-amber-800">{description}</p>
    </div>
  );
}

type HomeCatalog = {
  categories: HomeCategoryItem[];
  featured: ProductSummary[];
  bestSellers: ProductSummary[];
  newArrivals: ProductSummary[];
};

function resolvedValue<T>(result: PromiseSettledResult<T>, fallback: T): T {
  return result.status === "fulfilled" ? result.value : fallback;
}

function resolvedProductContent(
  result: PromiseSettledResult<{ content: ProductSummary[] }>,
): ProductSummary[] {
  return result.status === "fulfilled" ? result.value.content : [];
}

async function loadHomeCatalog(): Promise<HomeCatalog> {
  // Trang chủ là tập hợp nhiều truy vấn độc lập. Không để một truy vấn sản phẩm
  // lỗi tạm thời làm mất luôn danh mục và hiển thị sai rằng Catalog Service dừng.
  const [categoriesResult, featuredResult, bestSellersResult, newArrivalsResult] = await Promise.allSettled([
    getCategories(),
    getProducts({ featured: true, sort: "BEST_SELLER", page: 0, size: 8 }),
    getProducts({ sort: "BEST_SELLER", page: 0, size: 8 }),
    getProducts({ sort: "NEWEST", page: 0, size: 8 }),
  ]);

  const categories = resolvedValue(categoriesResult, []);

  return {
    categories: presentCategories(categories),
    featured: resolvedProductContent(featuredResult),
    bestSellers: resolvedProductContent(bestSellersResult),
    newArrivals: resolvedProductContent(newArrivalsResult),
  };
}

export async function HomePage() {
  const catalog = await loadHomeCatalog();
  return (
    <div className="overflow-hidden bg-[#f7f8f6]">
      <div className="border-b border-emerald-900/20 bg-emerald-950 text-emerald-50">
        <div className="mx-auto flex min-h-10 max-w-7xl items-center justify-center px-4 text-center text-xs font-semibold tracking-normal sm:justify-between sm:px-6 lg:px-8">
          <span>Miễn phí vận chuyển cho đơn từ 499.000₫</span>
          <span className="hidden sm:block">Hàng chính hãng · Đổi trả 7 ngày · Hỗ trợ mỗi ngày</span>
        </div>
      </div>

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
        <section className="scroll-mt-28" id="categories">{catalog.categories.length ? <CategoryCarousel items={catalog.categories} /> : <EmptyCatalogSection title="Chưa có danh mục để hiển thị" description="Danh mục sẽ xuất hiện ngay sau khi dữ liệu được đồng bộ." />}</section>

        <section>
          <SectionTitle description="8 sản phẩm nổi bật được đội ngũ chọn lọc, ưu tiên các lựa chọn có lượt mua tốt và giá giảm hấp dẫn." eyebrow="Đề xuất hôm nay" href="/products?featured=true&sort=best-selling" title="Sản phẩm nổi bật" />
          {catalog.featured.length ? <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 sm:gap-5">{catalog.featured.map((product) => <CatalogProductCard key={product.id} product={product} />)}</div> : <EmptyCatalogSection title="Chưa có sản phẩm nổi bật" description="Hãy đánh dấu sản phẩm nổi bật trong trang quản trị để hiển thị tại đây." />}
        </section>

        <section className="grid gap-4 lg:grid-cols-[1.35fr_.65fr]">
          <Link className="group relative min-h-80 overflow-hidden rounded-[2rem] bg-slate-950 p-7 text-white shadow-sm sm:p-10" href="/products?category=nha-cua-doi-song">
            <Image alt="Không gian sống tiện nghi" className="object-cover opacity-65 transition duration-700 group-hover:scale-105" fill sizes="(max-width: 1024px) 100vw, 68vw" src="https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=1400&q=84" />
            <div className="absolute inset-0 bg-gradient-to-r from-slate-950 via-slate-950/65 to-transparent" />
            <div className="relative flex h-full max-w-md flex-col justify-end">
              <p className="text-xs font-black tracking-[0.2em] text-emerald-300 uppercase">Không gian sống</p>
              <h2 className="mt-3 text-3xl font-black tracking-tight sm:text-4xl">Nhẹ nhàng nâng cấp góc sống mỗi ngày.</h2>
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

        <section id="best-seller">
          <SectionTitle description="8 sản phẩm có tổng số lượng bán cao nhất trong 7 ngày gần đây." eyebrow="Được săn đón" href="/products?sort=best-selling" title="Best seller tuần này" />
          {catalog.bestSellers.length ? <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 sm:gap-5">{catalog.bestSellers.map((product) => <CatalogProductCard key={product.id} product={product} />)}</div> : <EmptyCatalogSection title="Chưa có dữ liệu bán chạy" description="Danh sách sẽ được cập nhật khi có đơn hàng hoàn tất." />}
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
          <SectionTitle description="8 sản phẩm vừa được nhập và công bố gần nhất trên DynamicMart." eyebrow="Vừa lên kệ" href="/products?sort=newest" title="Sản phẩm mới về" />
          {catalog.newArrivals.length ? <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 sm:gap-5">{catalog.newArrivals.map((product) => <CatalogProductCard key={product.id} product={product} />)}</div> : <EmptyCatalogSection title="Chưa có sản phẩm mới về" description="Sản phẩm mới sẽ xuất hiện tại đây sau khi được công bố." />}
        </section>
      </div>
    </div>
  );
}
