import type { Metadata } from "next";
import { CatalogPage } from "@/features/catalog";
import type { CatalogFilters } from "@/features/catalog/types";

export const metadata: Metadata = { title: "Sản phẩm" };

function first(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}

function all(value: string | string[] | undefined) {
  if (!value) return undefined;
  return Array.isArray(value) ? value : [value];
}

export default async function ProductsPage({ searchParams }: Readonly<{ searchParams: Promise<Record<string, string | string[] | undefined>> }>) {
  const params = await searchParams;
  const filters: CatalogFilters = {
    q: first(params.q),
    category: first(params.category),
    sort: first(params.sort),
    minimumPriceVnd: first(params.minimumPriceVnd),
    maximumPriceVnd: first(params.maximumPriceVnd),
    attribute: all(params.attribute),
    page: first(params.page),
  };
  return <CatalogPage filters={filters} />;
}
