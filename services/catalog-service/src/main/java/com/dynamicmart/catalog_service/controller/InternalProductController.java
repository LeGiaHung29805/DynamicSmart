package com.dynamicmart.catalog_service.controller;

import com.dynamicmart.catalog_service.config.InternalApiVerifier;
import com.dynamicmart.catalog_service.dto.response.ApiResponse;
import com.dynamicmart.catalog_service.service.ProductManagementService;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/internal/products")
public class InternalProductController {
    private final ProductManagementService productManagementService;
    private final InternalApiVerifier internalApiVerifier;

    public InternalProductController(ProductManagementService productManagementService,
                                     InternalApiVerifier internalApiVerifier) {
        this.productManagementService = productManagementService;
        this.internalApiVerifier = internalApiVerifier;
    }

    @PutMapping("/{productId}/rating")
    public ApiResponse<Void> updateRating(
            @RequestHeader("X-Internal-Api-Key") String internalApiKey,
            @PathVariable UUID productId,
            @RequestBody UpdateRatingRequest request) {
        internalApiVerifier.require(internalApiKey);
        productManagementService.updateRating(productId, request.averageRating(), request.reviewCount());
        return ApiResponse.of(null);
    }

    public record UpdateRatingRequest(double averageRating, int reviewCount) {}
}
