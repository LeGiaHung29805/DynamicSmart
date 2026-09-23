package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.dto.response.PageResponse;
import com.dynamicmart.catalog_service.dto.response.ProductDetailResponse;
import com.dynamicmart.catalog_service.dto.response.ProductSummaryResponse;
import com.dynamicmart.catalog_service.dto.response.ProductVariantResponse;
import com.dynamicmart.catalog_service.entity.CatalogStatus;
import com.dynamicmart.catalog_service.entity.Product;
import com.dynamicmart.catalog_service.entity.ProductVariant;
import com.dynamicmart.catalog_service.entity.VariantStatus;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.query.CatalogProductQueryRepository;
import com.dynamicmart.catalog_service.query.ProductAttributeFilter;
import com.dynamicmart.catalog_service.query.ProductIdPage;
import com.dynamicmart.catalog_service.query.ProductSearchCriteria;
import com.dynamicmart.catalog_service.repository.CategoryRepository;
import com.dynamicmart.catalog_service.repository.ProductRepository;
import com.dynamicmart.catalog_service.repository.ProductVariantRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogQueryService {
    private static final Pattern ATTRIBUTE_CODE = Pattern.compile("^[A-Za-z0-9_-]+$");

    private final CatalogProductQueryRepository queryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final CatalogResponseAssembler responseAssembler;

    public CatalogQueryService(CatalogProductQueryRepository queryRepository,
                               ProductRepository productRepository,
                               ProductVariantRepository variantRepository,
                               CategoryRepository categoryRepository,
                               CatalogResponseAssembler responseAssembler) {
        this.queryRepository = queryRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.categoryRepository = categoryRepository;
        this.responseAssembler = responseAssembler;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> search(ProductSearchCriteria criteria) {
        validateCriteria(criteria);
        ProductIdPage result = queryRepository.search(criteria);
        Map<UUID, Product> productsById = new HashMap<>();
        productRepository.findAllById(result.productIds())
                .forEach(product -> productsById.put(product.getId(), product));
        List<Product> ordered = result.productIds().stream()
                .map(productsById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        List<ProductSummaryResponse> content = responseAssembler.productSummaries(
                ordered, Set.of(), Set.of());
        int totalPages = result.totalElements() == 0 ? 0
                : (int) Math.ceil((double) result.totalElements() / criteria.size());
        return new PageResponse<>(content, criteria.page(), criteria.size(), result.totalElements(),
                totalPages, criteria.page() == 0, criteria.page() + 1 >= totalPages);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getBySlug(String slug) {
        Product product = productRepository.findBySlugIgnoreCase(slug.trim().toLowerCase(Locale.ROOT))
                .filter(value -> value.isPublicAt(Instant.now()))
                .orElseThrow(this::productNotFound);
        boolean categoryActive = categoryRepository.findById(product.getCategoryId())
                .map(category -> category.getStatus() == CatalogStatus.ACTIVE)
                .orElse(false);
        if (!categoryActive) throw productNotFound();
        return responseAssembler.productDetail(product);
    }

    @Transactional(readOnly = true)
    public ProductVariantResponse getVariant(UUID variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .filter(value -> value.getStatus() == VariantStatus.ACTIVE)
                .orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND,
                        "VARIANT_NOT_FOUND", "Không tìm thấy Variant đang bán."));
        Product product = productRepository.findById(variant.getProductId())
                .filter(value -> value.isPublicAt(Instant.now()))
                .orElseThrow(this::productNotFound);
        return responseAssembler.productVariant(product, variant);
    }

    public List<ProductAttributeFilter> parseAttributeFilters(List<String> rawFilters) {
        if (rawFilters == null || rawFilters.isEmpty()) return List.of();
        if (rawFilters.size() > 20) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "TOO_MANY_ATTRIBUTE_FILTERS",
                    "Mỗi truy vấn chỉ được dùng tối đa 20 bộ lọc thuộc tính.");
        }
        List<ProductAttributeFilter> filters = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (String raw : rawFilters) {
            int separator = raw == null ? -1 : raw.indexOf(':');
            if (separator <= 0 || separator == raw.length() - 1) {
                throw invalidAttributeFilter();
            }
            String code = raw.substring(0, separator).trim();
            String value = raw.substring(separator + 1).trim();
            if (!ATTRIBUTE_CODE.matcher(code).matches() || value.isBlank() || value.length() > 200) {
                throw invalidAttributeFilter();
            }
            String key = code.toUpperCase(Locale.ROOT) + "\u0000" + value.toLowerCase(Locale.ROOT);
            if (unique.add(key)) filters.add(new ProductAttributeFilter(code, value));
        }
        return List.copyOf(filters);
    }

    private void validateCriteria(ProductSearchCriteria criteria) {
        if (criteria.page() < 0 || criteria.size() < 1 || criteria.size() > 100) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "PAGINATION_INVALID",
                    "Page phải từ 0 và size trong khoảng 1–100.");
        }
        if (criteria.minimumPriceVnd() != null && criteria.minimumPriceVnd() < 0
                || criteria.maximumPriceVnd() != null && criteria.maximumPriceVnd() < 0) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "PRICE_RANGE_INVALID",
                    "Khoảng giá không được âm.");
        }
        if (criteria.minimumPriceVnd() != null && criteria.maximumPriceVnd() != null
                && criteria.minimumPriceVnd() > criteria.maximumPriceVnd()) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "PRICE_RANGE_INVALID",
                    "Giá tối thiểu không được lớn hơn giá tối đa.");
        }
        if (criteria.keyword() != null && criteria.keyword().length() > 200) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "KEYWORD_TOO_LONG",
                    "Từ khóa tìm kiếm quá dài.");
        }
    }

    private CatalogException invalidAttributeFilter() {
        return new CatalogException(HttpStatus.BAD_REQUEST, "ATTRIBUTE_FILTER_INVALID",
                "Bộ lọc thuộc tính phải có dạng CODE:value.");
    }

    private CatalogException productNotFound() {
        return new CatalogException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                "Không tìm thấy sản phẩm đang bán.");
    }
}
