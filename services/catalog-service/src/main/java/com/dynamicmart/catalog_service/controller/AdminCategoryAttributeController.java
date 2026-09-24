package com.dynamicmart.catalog_service.controller;

import com.dynamicmart.catalog_service.dto.request.MapCategoryAttributeRequest;
import com.dynamicmart.catalog_service.dto.response.ApiResponse;
import com.dynamicmart.catalog_service.dto.response.CategoryAttributeResponse;
import com.dynamicmart.catalog_service.service.AttributeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/admin/categories/{categoryId}/attributes")
public class AdminCategoryAttributeController {
    private final AttributeService attributeService;

    public AdminCategoryAttributeController(AttributeService attributeService) {
        this.attributeService = attributeService;
    }

    @GetMapping
    public ApiResponse<List<CategoryAttributeResponse>> list(@PathVariable UUID categoryId) {
        return ApiResponse.of(attributeService.listCategoryMappings(categoryId));
    }

    @PutMapping
    public ApiResponse<CategoryAttributeResponse> upsert(
            @PathVariable UUID categoryId,
            @Valid @RequestBody MapCategoryAttributeRequest request) {
        return ApiResponse.of(attributeService.mapToCategory(categoryId, request));
    }

    @DeleteMapping("/{mappingId}")
    public ResponseEntity<Void> remove(@PathVariable UUID categoryId,
                                       @PathVariable UUID mappingId) {
        attributeService.unmapFromCategory(categoryId, mappingId);
        return ResponseEntity.noContent().build();
    }
}
