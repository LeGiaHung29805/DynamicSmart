package com.dynamicmart.catalog_service.query;

import java.util.List;
import java.util.UUID;

public record ProductSearchCriteria(
        String keyword,
        UUID categoryId,
        Long minimumPriceVnd,
        Long maximumPriceVnd,
        Boolean featured,
        List<ProductAttributeFilter> attributes,
        ProductSort sort,
        int page,
        int size
) {
}
