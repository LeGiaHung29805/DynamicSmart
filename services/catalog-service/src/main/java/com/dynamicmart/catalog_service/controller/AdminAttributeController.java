package com.dynamicmart.catalog_service.controller;

import com.dynamicmart.catalog_service.dto.request.CatalogStatusRequest;
import com.dynamicmart.catalog_service.dto.request.CreateAttributeRequest;
import com.dynamicmart.catalog_service.dto.request.UpdateAttributeRequest;
import com.dynamicmart.catalog_service.dto.request.UpsertAttributeOptionRequest;
import com.dynamicmart.catalog_service.dto.response.ApiResponse;
import com.dynamicmart.catalog_service.dto.response.AttributeOptionResponse;
import com.dynamicmart.catalog_service.dto.response.AttributeResponse;
import com.dynamicmart.catalog_service.service.AttributeService;
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
@RequestMapping("/api/v1/catalog/admin/attributes")
public class AdminAttributeController {
    private final AttributeService attributeService;

    public AdminAttributeController(AttributeService attributeService) {
        this.attributeService = attributeService;
    }

    @GetMapping
    public ApiResponse<List<AttributeResponse>> list() {
        return ApiResponse.of(attributeService.listForAdmin());
    }

    @GetMapping("/{attributeId}")
    public ApiResponse<AttributeResponse> get(@PathVariable UUID attributeId) {
        return ApiResponse.of(attributeService.get(attributeId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AttributeResponse>> create(
            @Valid @RequestBody CreateAttributeRequest request) {
        AttributeResponse created = attributeService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/catalog/admin/attributes/" + created.id()))
                .body(ApiResponse.of(created));
    }

    @PutMapping("/{attributeId}")
    public ApiResponse<AttributeResponse> update(@PathVariable UUID attributeId,
                                                 @Valid @RequestBody UpdateAttributeRequest request) {
        return ApiResponse.of(attributeService.update(attributeId, request));
    }

    @PatchMapping("/{attributeId}/status")
    public ApiResponse<AttributeResponse> setStatus(@PathVariable UUID attributeId,
                                                    @Valid @RequestBody CatalogStatusRequest request) {
        return ApiResponse.of(attributeService.setActive(attributeId, request.active()));
    }

    @PostMapping("/{attributeId}/options")
    public ResponseEntity<ApiResponse<AttributeOptionResponse>> createOption(
            @PathVariable UUID attributeId,
            @Valid @RequestBody UpsertAttributeOptionRequest request) {
        AttributeOptionResponse created = attributeService.createOption(attributeId, request);
        return ResponseEntity.created(URI.create("/api/v1/catalog/admin/attributes/" + attributeId
                        + "/options/" + created.id()))
                .body(ApiResponse.of(created));
    }

    @PutMapping("/{attributeId}/options/{optionId}")
    public ApiResponse<AttributeOptionResponse> updateOption(
            @PathVariable UUID attributeId,
            @PathVariable UUID optionId,
            @Valid @RequestBody UpsertAttributeOptionRequest request) {
        return ApiResponse.of(attributeService.updateOption(attributeId, optionId, request));
    }

    @PatchMapping("/{attributeId}/options/{optionId}/status")
    public ApiResponse<AttributeOptionResponse> setOptionStatus(
            @PathVariable UUID attributeId,
            @PathVariable UUID optionId,
            @Valid @RequestBody CatalogStatusRequest request) {
        return ApiResponse.of(attributeService.setOptionActive(attributeId, optionId, request.active()));
    }
}
