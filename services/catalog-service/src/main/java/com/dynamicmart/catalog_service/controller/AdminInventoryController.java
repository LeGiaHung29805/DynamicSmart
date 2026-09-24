package com.dynamicmart.catalog_service.controller;

import com.dynamicmart.catalog_service.dto.request.AdjustInventoryRequest;
import com.dynamicmart.catalog_service.dto.response.AdminInventoryItemResponse;
import com.dynamicmart.catalog_service.dto.response.ApiResponse;
import com.dynamicmart.catalog_service.dto.response.InventoryAdjustmentResponse;
import com.dynamicmart.catalog_service.dto.response.PageResponse;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.service.InventoryAdjustmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/catalog/admin/inventory")
public class AdminInventoryController {
    private final InventoryAdjustmentService inventoryService;

    public AdminInventoryController(InventoryAdjustmentService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminInventoryItemResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(inventoryService.list(page, size));
    }

    @PostMapping("/{variantId}/adjustments")
    public ApiResponse<InventoryAdjustmentResponse> adjust(
            @PathVariable UUID variantId,
            @RequestHeader("Idempotency-Key") UUID operationKey,
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody AdjustInventoryRequest request) {
        return ApiResponse.of(inventoryService.adjust(variantId, operationKey,
                requireAdminId(principal), request));
    }

    @GetMapping("/{variantId}/adjustments")
    public ApiResponse<PageResponse<InventoryAdjustmentResponse>> history(
            @PathVariable UUID variantId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(inventoryService.history(variantId, page, size));
    }

    private UUID requireAdminId(Jwt principal) {
        if (principal == null || principal.getSubject() == null) {
            throw new CatalogException(HttpStatus.UNAUTHORIZED, "ADMIN_PRINCIPAL_MISSING",
                    "Không xác định được quản trị viên thực hiện thao tác.");
        }
        try {
            return UUID.fromString(principal.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new CatalogException(HttpStatus.UNAUTHORIZED, "ADMIN_PRINCIPAL_INVALID",
                    "Định danh quản trị viên không hợp lệ.");
        }
    }
}
