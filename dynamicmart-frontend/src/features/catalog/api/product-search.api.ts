import { apiClient } from "@/lib/api/client";
import type { ProductSearchSuggestions } from "../types";

export function getProductSearchSuggestions(query: string, signal?: AbortSignal) {
  const params = new URLSearchParams({ q: query });
  return apiClient.get<ProductSearchSuggestions>(
    `/api/v1/catalog/products/suggestions?${params.toString()}`,
    { signal },
  );
}
