package com.dynamicmart.catalog_service.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogProductQueryRepository {
    private static final double FUZZY_THRESHOLD = 0.45D;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SearchTermNormalizer searchTermNormalizer;

    public CatalogProductQueryRepository(NamedParameterJdbcTemplate jdbcTemplate,
                                         SearchTermNormalizer searchTermNormalizer) {
        this.jdbcTemplate = jdbcTemplate;
        this.searchTermNormalizer = searchTermNormalizer;
    }

    public ProductIdPage search(ProductSearchCriteria criteria) {
        SearchTerms searchTerms = searchTermNormalizer.normalize(criteria.keyword());
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("now", Timestamp.from(Instant.now()))
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
        String fromAndWhere = buildFromAndWhere(criteria, parameters, searchTerms);
        String select = commonTableExpression + "SELECT p.id " + fromAndWhere
                + orderBy(criteria.sort(), searchTerms, parameters)
                + " LIMIT :limit OFFSET :offset";
        List<UUID> ids = jdbcTemplate.query(select, parameters,
                (resultSet, rowNumber) -> uuid(resultSet, "id"));
        Long total = jdbcTemplate.queryForObject(commonTableExpression + "SELECT COUNT(*) " + fromAndWhere,
                parameters, Long.class);
        return new ProductIdPage(ids, total == null ? 0 : total);
    }

    public SearchSuggestionRows suggest(String query, int productLimit, int categoryLimit) {
        SearchTerms searchTerms = searchTermNormalizer.normalize(query);
        if (searchTerms.isEmpty()) return SearchSuggestionRows.empty();

        MapSqlParameterSource productParameters = new MapSqlParameterSource()
                .addValue("now", Timestamp.from(Instant.now()))
                .addValue("productLimit", productLimit);
        String productPredicate = productKeywordPredicate(searchTerms, productParameters, "suggestProduct");
        addRankingParameters(productParameters, searchTerms, "productRank");
        String productSql = """
                SELECT p.id,
                       p.name,
                       p.slug,
                       category.name AS category_name,
                       category.slug AS category_slug,
                       (
                           SELECT image.image_url
                           FROM product_images image
                           WHERE image.product_id = p.id
                           ORDER BY CASE WHEN image.is_primary THEN 0 ELSE 1 END,
                                    CASE WHEN image.variant_id IS NULL THEN 0 ELSE 1 END,
                                    image.sort_order ASC,
                                    image.id ASC
                           LIMIT 1
                       ) AS image_url,
                       (
                           SELECT MIN(price_variant.price_vnd)
                           FROM product_variants price_variant
                           JOIN inventory_items price_inventory
                             ON price_inventory.variant_id = price_variant.id
                           WHERE price_variant.product_id = p.id
                             AND price_variant.status = 'ACTIVE'
                             AND price_inventory.on_hand_qty - price_inventory.reserved_qty > 0
                       ) AS minimum_price_vnd
                FROM products p
                JOIN categories category
                  ON category.id = p.category_id
                 AND category.status = 'ACTIVE'
                WHERE p.status = 'ACTIVE'
                  AND p.published_at IS NOT NULL
                  AND p.published_at <= :now
                  AND EXISTS (
                      SELECT 1
                      FROM product_variants candidate
                      JOIN inventory_items inventory ON inventory.variant_id = candidate.id
                      WHERE candidate.product_id = p.id
                        AND candidate.status = 'ACTIVE'
                        AND inventory.on_hand_qty - inventory.reserved_qty > 0
                        AND (
                """ + productPredicate + """
                        )
                  )
                """ + relevanceOrder("productRank") + " LIMIT :productLimit";
        List<ProductSuggestionRow> products = jdbcTemplate.query(productSql, productParameters,
                (resultSet, rowNumber) -> new ProductSuggestionRow(
                        uuid(resultSet, "id"),
                        resultSet.getString("name"),
                        resultSet.getString("slug"),
                        resultSet.getString("category_name"),
                        resultSet.getString("category_slug"),
                        resultSet.getString("image_url"),
                        nullableLong(resultSet, "minimum_price_vnd")
                ));

        MapSqlParameterSource categoryParameters = new MapSqlParameterSource()
                .addValue("now", Timestamp.from(Instant.now()))
                .addValue("categoryLimit", categoryLimit);
        String categoryPredicate = categoryKeywordPredicate(searchTerms, categoryParameters, "suggestCategory");
        addRankingParameters(categoryParameters, searchTerms, "categoryRank");
        String categorySql = """
                SELECT category.id, category.name, category.slug
                FROM categories category
                WHERE category.status = 'ACTIVE'
                  AND (
                """ + categoryPredicate + """
                  )
                  AND EXISTS (
                      WITH RECURSIVE descendant_categories AS (
                          SELECT category.id
                          UNION ALL
                          SELECT child.id
                          FROM categories child
                          JOIN descendant_categories parent ON child.parent_id = parent.id
                          WHERE child.status = 'ACTIVE'
                      )
                      SELECT 1
                      FROM products p
                      JOIN product_variants candidate ON candidate.product_id = p.id
                      JOIN inventory_items inventory ON inventory.variant_id = candidate.id
                      WHERE p.category_id IN (SELECT id FROM descendant_categories)
                        AND p.status = 'ACTIVE'
                        AND p.published_at IS NOT NULL
                        AND p.published_at <= :now
                        AND candidate.status = 'ACTIVE'
                        AND inventory.on_hand_qty - inventory.reserved_qty > 0
                  )
                ORDER BY
                    CASE
                        WHEN catalog_normalize_search_text(category.name) = :categoryRankTerm THEN 0
                        WHEN catalog_compact_search_text(category.name) = :categoryRankCompact THEN 1
                        WHEN catalog_normalize_search_text(category.name) LIKE :categoryRankPrefix THEN 2
                        WHEN catalog_compact_search_text(category.name) LIKE :categoryRankCompactPrefix THEN 3
                        ELSE 4
                    END,
                    word_similarity(:categoryRankTerm, catalog_normalize_search_text(category.name)) DESC,
                    category.sort_order ASC,
                    category.name ASC
                LIMIT :categoryLimit
                """;
        List<CategorySuggestionRow> categories = jdbcTemplate.query(categorySql, categoryParameters,
                (resultSet, rowNumber) -> new CategorySuggestionRow(
                        uuid(resultSet, "id"),
                        resultSet.getString("name"),
                        resultSet.getString("slug")
                ));
        return new SearchSuggestionRows(products, categories);
    }

    private String buildFromAndWhere(ProductSearchCriteria criteria,
                                     MapSqlParameterSource parameters,
                                     SearchTerms searchTerms) {
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
                    WHERE candidate.product_id = p.id
                      AND candidate.status = 'ACTIVE'
                """);
        if (criteria.minimumPriceVnd() != null) {
            parameters.addValue("minimumPrice", criteria.minimumPriceVnd());
            sql.append(" AND candidate.price_vnd >= :minimumPrice\n");
        }
        if (criteria.maximumPriceVnd() != null) {
            parameters.addValue("maximumPrice", criteria.maximumPriceVnd());
            sql.append(" AND candidate.price_vnd <= :maximumPrice\n");
        }
        if (!searchTerms.isEmpty()) {
            sql.append(" AND (\n")
                    .append(productKeywordPredicate(searchTerms, parameters, "search"))
                    .append(" )\n");
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

    private String productKeywordPredicate(SearchTerms searchTerms,
                                           MapSqlParameterSource parameters,
                                           String prefix) {
        List<String> alternatives = new ArrayList<>();
        for (int index = 0; index < searchTerms.normalized().size(); index++) {
            String term = searchTerms.normalized().get(index);
            String compact = term.replace(" ", "");
            String termParameter = prefix + "Term" + index;
            String likeParameter = prefix + "Like" + index;
            String compactLikeParameter = prefix + "CompactLike" + index;
            parameters.addValue(termParameter, term)
                    .addValue(likeParameter, "%" + term + "%")
                    .addValue(compactLikeParameter, "%" + compact + "%");
            String fuzzy = compact.length() >= 4 ? """
                     OR word_similarity(:%s, catalog_normalize_search_text(p.name)) >= :%sFuzzyThreshold
                     OR word_similarity(:%s, catalog_normalize_search_text(category.name)) >= :%sFuzzyThreshold
                     OR word_similarity(:%s, catalog_normalize_search_text(COALESCE(candidate.name, ''))) >= :%sFuzzyThreshold
                    """.formatted(termParameter, prefix, termParameter, prefix, termParameter, prefix) : "";
            alternatives.add("""
                    (
                        to_tsvector('simple', catalog_product_search_document(
                            p.name, p.slug, p.short_description, p.description
                        )) @@ plainto_tsquery('simple', :%s)
                        OR catalog_normalize_search_text(p.name) LIKE :%s
                        OR catalog_compact_search_text(p.name) LIKE :%s
                        OR catalog_normalize_search_text(category.name) LIKE :%s
                        OR catalog_normalize_search_text(category.slug) LIKE :%s
                        OR catalog_normalize_search_text(category.code) LIKE :%s
                        OR catalog_compact_search_text(category.name) LIKE :%s
                        OR catalog_normalize_search_text(candidate.sku) LIKE :%s
                        OR catalog_normalize_search_text(COALESCE(candidate.name, '')) LIKE :%s
                        OR catalog_compact_search_text(candidate.sku) LIKE :%s
                        OR catalog_compact_search_text(COALESCE(candidate.name, '')) LIKE :%s
                        %s
                    )
                    """.formatted(termParameter, likeParameter, compactLikeParameter,
                    likeParameter, likeParameter, likeParameter, compactLikeParameter,
                    likeParameter, likeParameter, compactLikeParameter, compactLikeParameter, fuzzy));
        }
        parameters.addValue(prefix + "FuzzyThreshold", FUZZY_THRESHOLD);
        return String.join(" OR\n", alternatives);
    }

    private String categoryKeywordPredicate(SearchTerms searchTerms,
                                            MapSqlParameterSource parameters,
                                            String prefix) {
        List<String> alternatives = new ArrayList<>();
        for (int index = 0; index < searchTerms.normalized().size(); index++) {
            String term = searchTerms.normalized().get(index);
            String compact = term.replace(" ", "");
            String termParameter = prefix + "Term" + index;
            String likeParameter = prefix + "Like" + index;
            String compactLikeParameter = prefix + "CompactLike" + index;
            parameters.addValue(termParameter, term)
                    .addValue(likeParameter, "%" + term + "%")
                    .addValue(compactLikeParameter, "%" + compact + "%");
            String fuzzy = compact.length() >= 4
                    ? " OR word_similarity(:" + termParameter
                    + ", catalog_normalize_search_text(category.name)) >= :"
                    + prefix + "FuzzyThreshold"
                    : "";
            alternatives.add("""
                    (
                        catalog_normalize_search_text(category.name) LIKE :%s
                        OR catalog_normalize_search_text(category.slug) LIKE :%s
                        OR catalog_normalize_search_text(category.code) LIKE :%s
                        OR catalog_normalize_search_text(COALESCE(category.description, '')) LIKE :%s
                        OR catalog_compact_search_text(category.name) LIKE :%s
                        %s
                    )
                    """.formatted(likeParameter, likeParameter, likeParameter,
                    likeParameter, compactLikeParameter, fuzzy));
        }
        parameters.addValue(prefix + "FuzzyThreshold", FUZZY_THRESHOLD);
        return String.join(" OR\n", alternatives);
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

    private String orderBy(ProductSort sort,
                           SearchTerms searchTerms,
                           MapSqlParameterSource parameters) {
        ProductSort safeSort = sort == null ? ProductSort.NEWEST : sort;
        String ordering = switch (safeSort) {
            case RELEVANCE -> {
                if (searchTerms.isEmpty()) yield newestOrder();
                addRankingParameters(parameters, searchTerms, "searchRank");
                yield relevanceOrder("searchRank");
            }
            case PRICE_ASC -> """
                     ORDER BY (
                        SELECT MIN(price_variant.price_vnd)
                        FROM product_variants price_variant
                        WHERE price_variant.product_id = p.id
                          AND price_variant.status = 'ACTIVE'
                     ) ASC, p.id ASC
                    """;
            case PRICE_DESC -> """
                     ORDER BY (
                        SELECT MIN(price_variant.price_vnd)
                        FROM product_variants price_variant
                        WHERE price_variant.product_id = p.id
                          AND price_variant.status = 'ACTIVE'
                     ) DESC, p.id ASC
                    """;
            case NEWEST, BEST_SELLER -> newestOrder();
        };
        return prioritizeAvailableProducts(ordering);
    }

    private String prioritizeAvailableProducts(String ordering) {
        String availabilityRank = """
                CASE WHEN EXISTS (
                    SELECT 1
                    FROM product_variants stock_variant
                    JOIN inventory_items stock_inventory ON stock_inventory.variant_id = stock_variant.id
                    WHERE stock_variant.product_id = p.id
                      AND stock_variant.status = 'ACTIVE'
                      AND stock_inventory.on_hand_qty - stock_inventory.reserved_qty > 0
                ) THEN 0 ELSE 1 END
                """.strip();
        return ordering.replaceFirst("ORDER BY", "ORDER BY " + availabilityRank + ",");
    }

    private String relevanceOrder(String prefix) {
        return """
                 ORDER BY
                    CASE
                        WHEN catalog_normalize_search_text(p.name) = :%1$sTerm THEN 0
                        WHEN catalog_compact_search_text(p.name) = :%1$sCompact THEN 1
                        WHEN catalog_normalize_search_text(p.name) LIKE :%1$sPrefix THEN 2
                        WHEN catalog_compact_search_text(p.name) LIKE :%1$sCompactPrefix THEN 3
                        ELSE 4
                    END,
                    ts_rank_cd(
                        to_tsvector('simple', catalog_product_search_document(
                            p.name, p.slug, p.short_description, p.description
                        )),
                        plainto_tsquery('simple', :%1$sTerm)
                    ) DESC,
                    word_similarity(:%1$sTerm, catalog_normalize_search_text(p.name)) DESC,
                    p.published_at DESC,
                    p.id ASC
                """.formatted(prefix);
    }

    private void addRankingParameters(MapSqlParameterSource parameters,
                                      SearchTerms searchTerms,
                                      String prefix) {
        String term = searchTerms.primary();
        String compact = term.replace(" ", "");
        parameters.addValue(prefix + "Term", term)
                .addValue(prefix + "Compact", compact)
                .addValue(prefix + "Prefix", term + "%")
                .addValue(prefix + "CompactPrefix", compact + "%");
    }

    private String newestOrder() {
        return " ORDER BY p.published_at DESC, p.created_at DESC, p.id ASC\n";
    }

    private UUID uuid(ResultSet resultSet, String column) throws SQLException {
        Object value = resultSet.getObject(column);
        return value instanceof UUID id ? id : UUID.fromString(value.toString());
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        Object value = resultSet.getObject(column);
        return value == null ? null : ((Number) value).longValue();
    }
}
