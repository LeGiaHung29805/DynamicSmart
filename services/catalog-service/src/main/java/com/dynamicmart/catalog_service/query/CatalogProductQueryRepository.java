package com.dynamicmart.catalog_service.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogProductQueryRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CatalogProductQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ProductIdPage search(ProductSearchCriteria criteria) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("now", Instant.now())
                .addValue("limit", criteria.size())
                .addValue("offset", Math.multiplyExact(criteria.page(), criteria.size()));
        String commonTableExpression = criteria.categoryId() == null ? "" : """
                WITH RECURSIVE selected_categories AS (
                    SELECT id FROM categories WHERE id = :categoryId AND status = 'ACTIVE'
                    UNION ALL
                    SELECT child.id
                    FROM categories child
                    JOIN selected_categories parent ON child.parent_id = parent.id
                    WHERE child.status = 'ACTIVE'
                )
                """;
        String fromAndWhere = buildFromAndWhere(criteria, parameters);
        String select = commonTableExpression + "SELECT p.id " + fromAndWhere + orderBy(criteria.sort())
                + " LIMIT :limit OFFSET :offset";
        List<UUID> ids = jdbcTemplate.query(select, parameters,
                (resultSet, rowNumber) -> uuid(resultSet, "id"));
        Long total = jdbcTemplate.queryForObject(commonTableExpression + "SELECT COUNT(*) " + fromAndWhere,
                parameters, Long.class);
        return new ProductIdPage(ids, total == null ? 0 : total);
    }

    private String buildFromAndWhere(ProductSearchCriteria criteria,
                                     MapSqlParameterSource parameters) {
        StringBuilder sql = new StringBuilder();
        if (criteria.categoryId() != null) {
            parameters.addValue("categoryId", criteria.categoryId());
        }
        sql.append("""
                FROM products p
                JOIN categories category ON category.id = p.category_id AND category.status = 'ACTIVE'
                WHERE p.status = 'ACTIVE'
                  AND p.published_at IS NOT NULL
                  AND p.published_at <= :now
                """);
        if (criteria.categoryId() != null) {
            sql.append(" AND p.category_id IN (SELECT id FROM selected_categories)\n");
        }
        if (criteria.featured() != null) {
            parameters.addValue("featured", criteria.featured());
            sql.append(" AND p.is_featured = :featured\n");
        }
        sql.append("""
                 AND EXISTS (
                    SELECT 1
                    FROM product_variants candidate
                    JOIN inventory_items inventory ON inventory.variant_id = candidate.id
                    WHERE candidate.product_id = p.id
                      AND candidate.status = 'ACTIVE'
                      AND inventory.on_hand_qty - inventory.reserved_qty > 0
                """);
        if (criteria.minimumPriceVnd() != null) {
            parameters.addValue("minimumPrice", criteria.minimumPriceVnd());
            sql.append(" AND candidate.price_vnd >= :minimumPrice\n");
        }
        if (criteria.maximumPriceVnd() != null) {
            parameters.addValue("maximumPrice", criteria.maximumPriceVnd());
            sql.append(" AND candidate.price_vnd <= :maximumPrice\n");
        }
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            parameters.addValue("keyword", "%" + criteria.keyword().trim().toLowerCase(Locale.ROOT) + "%");
            sql.append("""
                     AND (
                            LOWER(p.name) LIKE :keyword
                         OR LOWER(p.slug) LIKE :keyword
                         OR LOWER(COALESCE(p.short_description, '')) LIKE :keyword
                         OR LOWER(candidate.sku) LIKE :keyword
                         OR LOWER(COALESCE(candidate.name, '')) LIKE :keyword
                     )
                    """);
        }
        List<ProductAttributeFilter> filters = criteria.attributes() == null
                ? List.of() : criteria.attributes();
        for (int index = 0; index < filters.size(); index++) {
            ProductAttributeFilter filter = filters.get(index);
            String codeParameter = "attributeCode" + index;
            String valueParameter = "attributeValue" + index;
            parameters.addValue(codeParameter, filter.code().trim().toLowerCase(Locale.ROOT));
            parameters.addValue(valueParameter, filter.value().trim().toLowerCase(Locale.ROOT));
            sql.append(attributeFilter(codeParameter, valueParameter));
        }
        sql.append(" )\n");
        return sql.toString();
    }

    private String attributeFilter(String codeParameter, String valueParameter) {
        return """
                 AND (
                    EXISTS (
                        SELECT 1
                        FROM category_attributes mapping
                        JOIN attributes attribute ON attribute.id = mapping.attribute_id
                        JOIN product_attribute_values product_value
                          ON product_value.product_id = p.id
                         AND product_value.attribute_id = attribute.id
                        WHERE mapping.category_id = p.category_id
                          AND mapping.applies_to = 'PRODUCT'
                          AND attribute.status = 'ACTIVE'
                          AND LOWER(attribute.code) = :%s
                          AND %s
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM category_attributes mapping
                        JOIN attributes attribute ON attribute.id = mapping.attribute_id
                        JOIN variant_attribute_values variant_value
                          ON variant_value.variant_id = candidate.id
                         AND variant_value.attribute_id = attribute.id
                        WHERE mapping.category_id = p.category_id
                          AND mapping.applies_to = 'VARIANT'
                          AND attribute.status = 'ACTIVE'
                          AND LOWER(attribute.code) = :%s
                          AND %s
                    )
                 )
                """.formatted(codeParameter,
                jsonValueMatches("product_value.value_json", valueParameter),
                codeParameter, jsonValueMatches("variant_value.value_json", valueParameter));
    }

    private String jsonValueMatches(String column, String valueParameter) {
        return "(" +
                "LOWER(CASE WHEN jsonb_typeof(" + column + ") = 'string' " +
                "THEN " + column + " #>> '{}' ELSE " + column + "::text END) = :" + valueParameter +
                " OR EXISTS (SELECT 1 FROM jsonb_array_elements_text(" +
                "CASE WHEN jsonb_typeof(" + column + ") = 'array' THEN " + column +
                " ELSE '[]'::jsonb END) element WHERE LOWER(element.value) = :" + valueParameter + "))";
    }

    private String orderBy(ProductSort sort) {
        ProductSort safeSort = sort == null ? ProductSort.NEWEST : sort;
        return switch (safeSort) {
            case PRICE_ASC -> """
                     ORDER BY (
                        SELECT MIN(price_variant.price_vnd)
                        FROM product_variants price_variant
                        JOIN inventory_items price_inventory ON price_inventory.variant_id = price_variant.id
                        WHERE price_variant.product_id = p.id
                          AND price_variant.status = 'ACTIVE'
                          AND price_inventory.on_hand_qty - price_inventory.reserved_qty > 0
                     ) ASC, p.id ASC
                    """;
            case PRICE_DESC -> """
                     ORDER BY (
                        SELECT MIN(price_variant.price_vnd)
                        FROM product_variants price_variant
                        JOIN inventory_items price_inventory ON price_inventory.variant_id = price_variant.id
                        WHERE price_variant.product_id = p.id
                          AND price_variant.status = 'ACTIVE'
                          AND price_inventory.on_hand_qty - price_inventory.reserved_qty > 0
                     ) DESC, p.id ASC
                    """;
            case NEWEST, BEST_SELLER -> " ORDER BY p.published_at DESC, p.created_at DESC, p.id ASC\n";
        };
    }

    private UUID uuid(ResultSet resultSet, String column) throws SQLException {
        Object value = resultSet.getObject(column);
        return value instanceof UUID id ? id : UUID.fromString(value.toString());
    }
}
