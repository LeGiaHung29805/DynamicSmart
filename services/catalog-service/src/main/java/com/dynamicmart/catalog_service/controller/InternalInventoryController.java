package com.dynamicmart.catalog_service.controller;

import com.dynamicmart.catalog_service.config.InternalApiVerifier;
import com.dynamicmart.catalog_service.dto.request.CommitInventoryRequest;
import com.dynamicmart.catalog_service.dto.request.ReleaseInventoryRequest;
import com.dynamicmart.catalog_service.dto.request.ReserveInventoryRequest;
import com.dynamicmart.catalog_service.dto.response.ApiResponse;
import com.dynamicmart.catalog_service.dto.response.InventoryReservationResponse;
import com.dynamicmart.catalog_service.service.InventoryReservationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/internal/inventory/reservations")
public class InternalInventoryController {
    private final InventoryReservationService reservationService;
    private final InternalApiVerifier internalApiVerifier;

    public InternalInventoryController(InventoryReservationService reservationService,
                                       InternalApiVerifier internalApiVerifier) {
        this.reservationService = reservationService;
        this.internalApiVerifier = internalApiVerifier;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryReservationResponse>> reserve(
            @RequestHeader("X-Internal-Api-Key") String internalApiKey,
            @RequestHeader("Idempotency-Key") UUID operationKey,
            @Valid @RequestBody ReserveInventoryRequest request) {
        internalApiVerifier.require(internalApiKey);
        InventoryReservationResponse response = reservationService.reserve(operationKey, request);
        return ResponseEntity.created(URI.create("/api/v1/catalog/internal/inventory/reservations/"
                        + response.reservationId()))
                .body(ApiResponse.of(response));
    }

    @PostMapping("/{reservationId}/commit")
    public ApiResponse<InventoryReservationResponse> commit(
            @RequestHeader("X-Internal-Api-Key") String internalApiKey,
            @RequestHeader("Idempotency-Key") UUID operationKey,
            @PathVariable UUID reservationId,
            @Valid @RequestBody CommitInventoryRequest request) {
        internalApiVerifier.require(internalApiKey);
        return ApiResponse.of(reservationService.commit(reservationId, operationKey, request));
    }

    @PostMapping("/{reservationId}/release")
    public ApiResponse<InventoryReservationResponse> release(
            @RequestHeader("X-Internal-Api-Key") String internalApiKey,
            @RequestHeader("Idempotency-Key") UUID operationKey,
            @PathVariable UUID reservationId,
            @Valid @RequestBody ReleaseInventoryRequest request) {
        internalApiVerifier.require(internalApiKey);
        return ApiResponse.of(reservationService.release(reservationId, operationKey, request));
    }

    @GetMapping("/{reservationId}")
    public ApiResponse<InventoryReservationResponse> get(
            @RequestHeader("X-Internal-Api-Key") String internalApiKey,
            @PathVariable UUID reservationId) {
        internalApiVerifier.require(internalApiKey);
        return ApiResponse.of(reservationService.get(reservationId));
    }
}
