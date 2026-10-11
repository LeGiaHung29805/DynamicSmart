package com.dynamicmart.catalog_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dynamicmart.catalog_service.dto.response.ProductSearchSuggestionsResponse;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.query.CatalogProductQueryRepository;
import com.dynamicmart.catalog_service.query.CategorySuggestionRow;
import com.dynamicmart.catalog_service.query.ProductSuggestionRow;
import com.dynamicmart.catalog_service.query.ProductAttributeFilter;
import com.dynamicmart.catalog_service.query.SearchSuggestionRows;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogQueryServiceTest {
    private final CatalogQueryService service = new CatalogQueryService(null, null, null, null, null, null);

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

    @Test
    void returnsNoSuggestionsWithoutQueryingDatabaseForOneCharacter() {
        CatalogProductQueryRepository repository = mock(CatalogProductQueryRepository.class);
        CatalogQueryService queryService = new CatalogQueryService(
                repository, null, null, null, null, null);

        ProductSearchSuggestionsResponse response = queryService.suggest("a");

        assertTrue(response.products().isEmpty());
        assertTrue(response.categories().isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void mapsProductAndCategorySuggestions() {
        CatalogProductQueryRepository repository = mock(CatalogProductQueryRepository.class);
        CatalogQueryService queryService = new CatalogQueryService(
                repository, null, null, null, null, null);
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(repository.suggest("ao thun", 6, 3)).thenReturn(new SearchSuggestionRows(
                List.of(new ProductSuggestionRow(productId, "Áo thun nam", "ao-thun-nam",
                        "Thời trang nam", "thoi-trang-nam", "/ao-thun.jpg", 199_000L)),
                List.of(new CategorySuggestionRow(categoryId, "Áo nam", "ao-nam"))));

        ProductSearchSuggestionsResponse response = queryService.suggest("  ao thun  ");

        assertEquals(productId, response.products().get(0).id());
        assertEquals(199_000L, response.products().get(0).minimumPriceVnd());
        assertEquals(categoryId, response.categories().get(0).id());
    }
}
