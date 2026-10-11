package com.dynamicmart.catalog_service.query;

import java.util.List;

public record SearchSuggestionRows(
        List<ProductSuggestionRow> products,
        List<CategorySuggestionRow> categories
) {
    public static SearchSuggestionRows empty() {
        return new SearchSuggestionRows(List.of(), List.of());
    }
}
