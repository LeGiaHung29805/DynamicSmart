package com.dynamicmart.catalog_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.query.ProductAttributeFilter;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogQueryServiceTest {
    private final CatalogQueryService service = new CatalogQueryService(null, null, null, null, null);

    @Test
    void parsesAndDeduplicatesAttributeFilters() {
        List<ProductAttributeFilter> filters = service.parseAttributeFilters(
                List.of("COLOR:red", "SIZE:XL", "color:RED"));

        assertEquals(2, filters.size());
        assertEquals("COLOR", filters.get(0).code());
        assertEquals("red", filters.get(0).value());
    }

    @Test
    void rejectsMalformedAttributeFilter() {
        CatalogException exception = assertThrows(CatalogException.class,
                () -> service.parseAttributeFilters(List.of("COLOR")));

        assertEquals("ATTRIBUTE_FILTER_INVALID", exception.getCode());
    }
}
