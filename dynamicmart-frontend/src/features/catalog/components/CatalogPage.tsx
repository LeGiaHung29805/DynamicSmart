import { EmptyState } from "@/components/common/PageState";
import { PageHeader } from "@/components/common/PageHeader";
import { ProductCard } from "@/features/home";
import { demoProducts } from "../demoProducts";
import { matchesPrice } from "../filterOptions";
import type { CatalogFilters } from "../types";
import { CatalogFilterControls } from "./CatalogFilterControls";

export function CatalogPage({ filters }: Readonly<{ filters: CatalogFilters }>) {
  const query = filters.q?.trim().toLocaleLowerCase("vi-VN") ?? "";
  const category = filters.category ?? "";
  const brand = filters.brand?.toLocaleLowerCase("vi-VN") ?? "";
  const price = filters.price ?? "";
  const sort = filters.sort ?? "featured";
  const promotion = filters.promotion === "true";

  const products = demoProducts
    .filter((product) => (!query || `${product.name} ${product.brand}`.toLocaleLowerCase("vi-VN").includes(query))
      && (!category || product.category === category)
      && (!brand || product.brand.toLocaleLowerCase("vi-VN") === brand)
      && (!price || matchesPrice(product.price, price))
      && (!promotion || product.oldPrice > product.price))
    .sort((a, b) => {
      if (sort === "price-asc") return a.price - b.price;
      if (sort === "price-desc") return b.price - a.price;
      if (sort === "newest") return Number(b.isNew) - Number(a.isNew);
      if (sort === "best-selling") return Number(a.isNew) - Number(b.isNew);
      return 0;
    });

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8 lg:py-14">
      <PageHeader eyebrow="Khám phá DynamicMart" title="Tìm đúng thứ bạn yêu thích" description="Tuyển chọn theo danh mục, thương hiệu và mức giá — một trải nghiệm mua sắm gọn gàng hơn cho bạn." />
      <div className="mt-8">
        <CatalogFilterControls count={products.length} filters={filters} key={JSON.stringify(filters)}>
          {products.length ? <div className="grid grid-cols-2 gap-3 sm:gap-5 md:grid-cols-3">{products.map((product) => <ProductCard key={product.id} product={product} />)}</div>
            : <EmptyState description="Không có sản phẩm minh họa phù hợp. Hãy thử bộ lọc khác; dữ liệu thực sẽ xuất hiện khi kết nối Catalog API." title="Chưa tìm thấy sản phẩm" />}
        </CatalogFilterControls>
      </div>
    </div>
  );
}
