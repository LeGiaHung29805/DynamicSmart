package com.dynamicmart.catalog_service.query;

import java.util.List;

public record SearchTerms(List<String> normalized, List<String> compact) {
    public static SearchTerms empty() {
        return new SearchTerms(List.of(), List.of());
    }

    public boolean isEmpty() {
        return normalized.isEmpty();
    }

    public String primary() {
        return normalized.isEmpty() ? "" : normalized.get(0);
    }
}
