package com.dynamicmart.catalog_service.controller;

import com.dynamicmart.catalog_service.dto.response.ApiResponse;
import com.dynamicmart.catalog_service.dto.response.PageResponse;
import com.dynamicmart.catalog_service.dto.response.ProductDetailResponse;
import com.dynamicmart.catalog_service.dto.response.ProductSummaryResponse;
import com.dynamicmart.catalog_service.dto.response.ProductVariantResponse;
import com.dynamicmart.catalog_service.query.ProductSearchCriteria;
import com.dynamicmart.catalog_service.query.ProductSort;
import com.dynamicmart.catalog_service.service.CatalogQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/catalog")
public class PublicProductController {
    private final CatalogQueryService queryService;

    public PublicProductController(CatalogQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductSummaryResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) @Min(0) Long minimumPriceVnd,
            @RequestParam(required = false) @Min(0) Long maximumPriceVnd,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(name = "attribute", required = false) List<String> attributes,
            @RequestParam(defaultValue = "NEWEST") ProductSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        ProductSearchCriteria criteria = new ProductSearchCriteria(keyword, categoryId,
                minimumPriceVnd, maximumPriceVnd, featured,
                queryService.parseAttributeFilters(attributes), sort, page, size);
        return ApiResponse.of(queryService.search(criteria));
    }

    @GetMapping("/products/{slug}")
    public ApiResponse<ProductDetailResponse> detail(@PathVariable String slug) {
        return ApiResponse.of(queryService.getBySlug(slug));
    }

    @GetMapping("/variants/{variantId}")
    public ApiResponse<ProductVariantResponse> variant(@PathVariable UUID variantId) {
        return ApiResponse.of(queryService.getVariant(variantId));
    }
}
