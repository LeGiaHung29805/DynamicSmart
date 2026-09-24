package com.dynamicmart.catalog_service.controller;

import com.dynamicmart.catalog_service.dto.request.CatalogStatusRequest;
import com.dynamicmart.catalog_service.dto.request.CreateCategoryRequest;
import com.dynamicmart.catalog_service.dto.request.UpdateCategoryRequest;
import com.dynamicmart.catalog_service.dto.response.ApiResponse;
import com.dynamicmart.catalog_service.dto.response.CategoryResponse;
import com.dynamicmart.catalog_service.service.CategoryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/admin/categories")
public class AdminCategoryController {
    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.of(categoryService.listForAdmin());
    }

    @GetMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> get(@PathVariable UUID categoryId) {
        return ApiResponse.of(categoryService.get(categoryId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse created = categoryService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/catalog/admin/categories/" + created.id()))
                .body(ApiResponse.of(created));
    }

    @PutMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> update(@PathVariable UUID categoryId,
                                                @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.of(categoryService.update(categoryId, request));
    }

    @PatchMapping("/{categoryId}/status")
    public ApiResponse<CategoryResponse> setStatus(@PathVariable UUID categoryId,
                                                   @Valid @RequestBody CatalogStatusRequest request) {
        return ApiResponse.of(categoryService.setActive(categoryId, request.active()));
    }
}
