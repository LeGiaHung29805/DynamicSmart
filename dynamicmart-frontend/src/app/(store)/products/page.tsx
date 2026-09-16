import type { Metadata } from "next";
import { CatalogPage } from "@/features/catalog";
import type { CatalogFilters } from "@/features/catalog/types";

export const metadata: Metadata = { title: "Sản phẩm" };

function first(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}

export default async function ProductsPage({ searchParams }: Readonly<{ searchParams: Promise<Record<string, string | string[] | undefined>> }>) {
  const params = await searchParams;
  const filters: CatalogFilters = {
    q: first(params.q), category: first(params.category), brand: first(params.brand),
    sort: first(params.sort), promotion: first(params.promotion), price: first(params.price),
  };
  return <CatalogPage filters={filters} />;
}
