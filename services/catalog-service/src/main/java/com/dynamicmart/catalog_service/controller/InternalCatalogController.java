package com.dynamicmart.catalog_service.controller;

import com.dynamicmart.catalog_service.config.InternalApiVerifier;
import com.dynamicmart.catalog_service.dto.request.ValidateVariantsRequest;
import com.dynamicmart.catalog_service.dto.response.ApiResponse;
import com.dynamicmart.catalog_service.dto.response.PurchasableVariantResponse;
import com.dynamicmart.catalog_service.service.PurchasableVariantService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/internal/variants")
public class InternalCatalogController {
    private final PurchasableVariantService purchasableVariantService;
    private final InternalApiVerifier internalApiVerifier;

    public InternalCatalogController(PurchasableVariantService purchasableVariantService,
                                     InternalApiVerifier internalApiVerifier) {
        this.purchasableVariantService = purchasableVariantService;
        this.internalApiVerifier = internalApiVerifier;
    }

    @PostMapping("/validate")
    public ApiResponse<List<PurchasableVariantResponse>> validate(
            @RequestHeader("X-Internal-Api-Key") String internalApiKey,
            @Valid @RequestBody ValidateVariantsRequest request) {
        internalApiVerifier.require(internalApiKey);
        return ApiResponse.of(purchasableVariantService.validate(request.items()));
    }
}
