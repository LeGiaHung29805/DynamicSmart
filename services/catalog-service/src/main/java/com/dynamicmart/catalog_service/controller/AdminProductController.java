package com.dynamicmart.catalog_service.controller;

import com.dynamicmart.catalog_service.dto.request.CreateProductRequest;
import com.dynamicmart.catalog_service.dto.request.CreateVariantRequest;
import com.dynamicmart.catalog_service.dto.request.ProductStatusRequest;
import com.dynamicmart.catalog_service.dto.request.UpdateProductRequest;
import com.dynamicmart.catalog_service.dto.request.UpdateVariantRequest;
import com.dynamicmart.catalog_service.dto.request.UpsertProductImageRequest;
import com.dynamicmart.catalog_service.dto.request.VariantStatusRequest;
import com.dynamicmart.catalog_service.dto.response.AdminProductResponse;
import com.dynamicmart.catalog_service.dto.response.ApiResponse;
import com.dynamicmart.catalog_service.dto.response.PageResponse;
import com.dynamicmart.catalog_service.dto.response.ProductImageResponse;
import com.dynamicmart.catalog_service.dto.response.ProductVariantResponse;
import com.dynamicmart.catalog_service.entity.ProductStatus;
import com.dynamicmart.catalog_service.service.ProductManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/catalog/admin/products")
public class AdminProductController {
    private final ProductManagementService productService;

    public AdminProductController(ProductManagementService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminProductResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(productService.listProducts(keyword, status, page, size));
    }

    @GetMapping("/{productId}")
    public ApiResponse<AdminProductResponse> get(@PathVariable UUID productId) {
        return ApiResponse.of(productService.getProduct(productId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminProductResponse>> create(
            @Valid @RequestBody CreateProductRequest request) {
        AdminProductResponse created = productService.createProduct(request);
        return ResponseEntity.created(URI.create("/api/v1/catalog/admin/products/" + created.id()))
                .body(ApiResponse.of(created));
    }

    @PutMapping("/{productId}")
    public ApiResponse<AdminProductResponse> update(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.of(productService.updateProduct(productId, request));
    }

    @PatchMapping("/{productId}/status")
    public ApiResponse<AdminProductResponse> setStatus(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductStatusRequest request) {
        return ApiResponse.of(productService.setProductStatus(productId, request));
    }

    @PostMapping("/{productId}/variants")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(
            @PathVariable UUID productId,
            @Valid @RequestBody CreateVariantRequest request) {
        ProductVariantResponse created = productService.createVariant(productId, request);
        return ResponseEntity.created(URI.create("/api/v1/catalog/admin/products/" + productId
                        + "/variants/" + created.id()))
                .body(ApiResponse.of(created));
    }

    @PutMapping("/{productId}/variants/{variantId}")
    public ApiResponse<ProductVariantResponse> updateVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateVariantRequest request) {
        return ApiResponse.of(productService.updateVariant(productId, variantId, request));
    }

    @PatchMapping("/{productId}/variants/{variantId}/status")
    public ApiResponse<ProductVariantResponse> setVariantStatus(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody VariantStatusRequest request) {
        return ApiResponse.of(productService.setVariantStatus(productId, variantId, request.status()));
    }

    @PostMapping("/{productId}/images")
    public ResponseEntity<ApiResponse<ProductImageResponse>> createImage(
            @PathVariable UUID productId,
            @Valid @RequestBody UpsertProductImageRequest request) {
        ProductImageResponse created = productService.createImage(productId, request);
        return ResponseEntity.created(URI.create("/api/v1/catalog/admin/products/" + productId
                        + "/images/" + created.id()))
                .body(ApiResponse.of(created));
    }

    @PutMapping("/{productId}/images/{imageId}")
    public ApiResponse<ProductImageResponse> updateImage(
            @PathVariable UUID productId,
            @PathVariable UUID imageId,
            @Valid @RequestBody UpsertProductImageRequest request) {
        return ApiResponse.of(productService.updateImage(productId, imageId, request));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID productId,
                                            @PathVariable UUID imageId) {
        productService.deleteImage(productId, imageId);
        return ResponseEntity.noContent().build();
    }
}
