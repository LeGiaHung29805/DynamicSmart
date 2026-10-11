package com.dynamicmart.catalog_service.dto.response;

import java.util.List;

public record ProductSearchSuggestionsResponse(
        List<ProductSearchSuggestionResponse> products,
        List<CategorySearchSuggestionResponse> categories
) {
    public static ProductSearchSuggestionsResponse empty() {
        return new ProductSearchSuggestionsResponse(List.of(), List.of());
    }
}
