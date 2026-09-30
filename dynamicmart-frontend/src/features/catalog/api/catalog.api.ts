import { serverApiRequest } from "@/lib/api/server";
import type {
  CatalogFilterDefinition,
  CategoryTree,
  PageResponse,
  ProductDetail,
  ProductSummary,
} from "../types";

export interface ProductQuery {
  keyword?: string;
  categoryId?: string;
  minimumPriceVnd?: number;
  maximumPriceVnd?: number;
  featured?: boolean;
  attributes?: string[];
  sort?: "NEWEST" | "PRICE_ASC" | "PRICE_DESC" | "BEST_SELLER";
  page?: number;
  size?: number;
}

export function getCategories() {
  return serverApiRequest<CategoryTree[]>("/api/v1/catalog/categories");
}

export function getCategoryFilters(slug: string) {
  return serverApiRequest<CatalogFilterDefinition[]>(
    `/api/v1/catalog/categories/${encodeURIComponent(slug)}/filters`,
  );
}

export function getProducts(query: ProductQuery = {}) {
  const params = new URLSearchParams();
  if (query.keyword) params.set("keyword", query.keyword);
  if (query.categoryId) params.set("categoryId", query.categoryId);
  if (query.minimumPriceVnd !== undefined) params.set("minimumPriceVnd", String(query.minimumPriceVnd));
  if (query.maximumPriceVnd !== undefined) params.set("maximumPriceVnd", String(query.maximumPriceVnd));
  if (query.featured !== undefined) params.set("featured", String(query.featured));
  query.attributes?.forEach((attribute) => params.append("attribute", attribute));
  params.set("sort", query.sort ?? "NEWEST");
  params.set("page", String(query.page ?? 0));
  params.set("size", String(query.size ?? 20));
  return serverApiRequest<PageResponse<ProductSummary>>(`/api/v1/catalog/products?${params.toString()}`);
}

export function getProductDetail(slug: string) {
  return serverApiRequest<ProductDetail>(`/api/v1/catalog/products/${encodeURIComponent(slug)}`);
}
