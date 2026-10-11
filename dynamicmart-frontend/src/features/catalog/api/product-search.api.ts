import { apiRequest } from "@/lib/api/request";
import type { ProductSearchSuggestions } from "../types";

export function getProductSearchSuggestions(query: string, signal?: AbortSignal) {
  const params = new URLSearchParams({ q: query });
  return apiRequest<ProductSearchSuggestions>(
    `/api/v1/catalog/products/suggestions?${params.toString()}`,
    { method: "GET", signal },
  );
}
