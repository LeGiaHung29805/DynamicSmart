package com.dynamicmart.catalog_service.query;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class CatalogProductQueryRepositoryTest {
    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void buildsRecursiveCategoryAndDynamicAttributeQuery() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class),
                org.mockito.ArgumentMatchers.<RowMapper<UUID>>any())).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(0L);
        CatalogProductQueryRepository repository = new CatalogProductQueryRepository(jdbcTemplate);
        ProductSearchCriteria criteria = new ProductSearchCriteria("iphone", UUID.randomUUID(),
                1_000_000L, 30_000_000L, true,
                List.of(new ProductAttributeFilter("COLOR", "black")),
                ProductSort.PRICE_ASC, 0, 20);

        repository.search(criteria);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), any(MapSqlParameterSource.class),
                org.mockito.ArgumentMatchers.<RowMapper<UUID>>any());
        assertTrue(sql.getValue().startsWith("WITH RECURSIVE selected_categories"));
        assertTrue(sql.getValue().contains("variant_attribute_values"));
        assertTrue(sql.getValue().contains("inventory.on_hand_qty - inventory.reserved_qty > 0"));
        assertTrue(sql.getValue().contains("ORDER BY ("));
    }
}
