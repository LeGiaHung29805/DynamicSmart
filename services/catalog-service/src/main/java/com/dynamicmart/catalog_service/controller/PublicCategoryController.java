package com.dynamicmart.catalog_service.controller;

import com.dynamicmart.catalog_service.dto.response.ApiResponse;
import com.dynamicmart.catalog_service.dto.response.CategoryResponse;
import com.dynamicmart.catalog_service.dto.response.CategoryTreeResponse;
import com.dynamicmart.catalog_service.dto.response.CatalogFilterDefinitionResponse;
import com.dynamicmart.catalog_service.service.AttributeService;
import com.dynamicmart.catalog_service.service.CategoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/categories")
public class PublicCategoryController {
    private final CategoryService categoryService;
    private final AttributeService attributeService;

    public PublicCategoryController(CategoryService categoryService, AttributeService attributeService) {
        this.categoryService = categoryService;
        this.attributeService = attributeService;
    }

    @GetMapping
    public ApiResponse<List<CategoryTreeResponse>> list() {
        return ApiResponse.of(categoryService.listActiveTree());
    }

    @GetMapping("/{slug}")
    public ApiResponse<CategoryResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.of(categoryService.getBySlug(slug));
    }

    @GetMapping("/{slug}/filters")
    public ApiResponse<List<CatalogFilterDefinitionResponse>> filters(@PathVariable String slug) {
        return ApiResponse.of(attributeService.listPublicFilters(slug));
    }
}
