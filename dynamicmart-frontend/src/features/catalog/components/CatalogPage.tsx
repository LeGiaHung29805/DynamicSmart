import Link from "next/link";
import { EmptyState, ErrorState } from "@/components/common/PageState";
import { PageHeader } from "@/components/common/PageHeader";
import { getCategories, getCategoryFilters, getProducts } from "../api/catalog.api";
import type { CatalogFilters, CategoryTree } from "../types";
import { CatalogFilterControls, type CategoryOption } from "./CatalogFilterControls";
import { CatalogProductCard } from "./CatalogProductCard";

function flattenCategories(categories: CategoryTree[], depth = 0): CategoryOption[] {
  return categories.flatMap((category) => [
    { id: category.id, label: category.name, slug: category.slug, depth },
    ...flattenCategories(category.children, depth + 1),
  ]);
}

function positiveNumber(value: string | undefined): number | undefined {
  if (!value) return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : undefined;
}

function apiSort(value: string | undefined) {
  if (value === "price-asc") return "PRICE_ASC" as const;
  if (value === "price-desc") return "PRICE_DESC" as const;
  if (value === "best-selling") return "BEST_SELLER" as const;
  return "NEWEST" as const;
}

function pageHref(filters: CatalogFilters, page: number): string {
  const params = new URLSearchParams();
  if (filters.q) params.set("q", filters.q);
  if (filters.category) params.set("category", filters.category);
  if (filters.minimumPriceVnd) params.set("minimumPriceVnd", filters.minimumPriceVnd);
  if (filters.maximumPriceVnd) params.set("maximumPriceVnd", filters.maximumPriceVnd);
  filters.attribute?.forEach((value) => params.append("attribute", value));
  if (filters.sort && filters.sort !== "newest") params.set("sort", filters.sort);
  if (page > 1) params.set("page", String(page));
  return `/products${params.size ? `?${params.toString()}` : ""}`;
}

export async function CatalogPage({ filters }: Readonly<{ filters: CatalogFilters }>) {
  let categoryTree;
  try {
    categoryTree = await getCategories();
  } catch {
    categoryTree = null;
  }
  if (!categoryTree) {
    return <div className="mx-auto w-full max-w-7xl px-4 py-12 sm:px-6 lg:px-8"><ErrorState description="Catalog API hiện chưa phản hồi. Hãy kiểm tra Gateway và catalog-service rồi tải lại trang." title="Không thể tải danh sách sản phẩm" /></div>;
  }
  const categories = flattenCategories(categoryTree);
  const selectedCategory = filters.category
    ? categories.find((category) => category.slug === filters.category)
    : undefined;
  if (filters.category && !selectedCategory) {
    return <div className="mx-auto w-full max-w-7xl px-4 py-12 sm:px-6 lg:px-8"><EmptyState description="Danh mục bạn chọn không tồn tại hoặc đã ngừng hoạt động." title="Không tìm thấy danh mục" /></div>;
  }
  const requestedPage = Math.max(Number.parseInt(filters.page ?? "1", 10) || 1, 1);
  const catalogData = await Promise.all([
      getProducts({
        keyword: filters.q?.trim() || undefined,
        categoryId: selectedCategory?.id,
        minimumPriceVnd: positiveNumber(filters.minimumPriceVnd),
        maximumPriceVnd: positiveNumber(filters.maximumPriceVnd),
        attributes: filters.attribute,
        sort: apiSort(filters.sort),
        page: requestedPage - 1,
        size: 18,
      }),
      selectedCategory ? getCategoryFilters(selectedCategory.slug) : Promise.resolve([]),
    ]).catch(() => null);
  if (!catalogData) {
    return <div className="mx-auto w-full max-w-7xl px-4 py-12 sm:px-6 lg:px-8"><ErrorState description="Catalog API hiện chưa phản hồi. Hãy kiểm tra Gateway và catalog-service rồi tải lại trang." title="Không thể tải danh sách sản phẩm" /></div>;
  }
  const [result, definitions] = catalogData;

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8 lg:py-14">
      <PageHeader eyebrow="Khám phá DynamicMart" title="Tìm đúng sản phẩm bạn cần" description="Tìm kiếm, lọc thuộc tính và sắp xếp đều chạy trên máy chủ để kết quả luôn chính xác với giá và tồn kho hiện tại." />
      <div className="mt-8">
        <CatalogFilterControls categories={categories} definitions={definitions} filters={filters} total={result.totalElements}>
          {result.content.length ? (
            <>
              <div className="grid grid-cols-2 gap-3 sm:gap-5 md:grid-cols-3">{result.content.map((product) => <CatalogProductCard key={product.id} product={product} />)}</div>
              {result.totalPages > 1 ? <nav aria-label="Phân trang sản phẩm" className="flex items-center justify-center gap-2 pt-5">
                {!result.first ? <Link className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-bold hover:border-emerald-300" href={pageHref(filters, result.page)}>Trang trước</Link> : null}
                <span className="px-3 text-sm text-slate-500">Trang {result.page + 1} / {result.totalPages}</span>
                {!result.last ? <Link className="rounded-xl bg-emerald-950 px-4 py-2 text-sm font-bold text-white hover:bg-brand" href={pageHref(filters, result.page + 2)}>Trang sau</Link> : null}
              </nav> : null}
            </>
          ) : <EmptyState description="Hãy bỏ bớt điều kiện hoặc thử một từ khóa khác." title="Chưa tìm thấy sản phẩm phù hợp" />}
        </CatalogFilterControls>
      </div>
    </div>
  );
}
