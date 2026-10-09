package com.dynamicmart.order_service.controller;

import com.dynamicmart.order_service.service.OrderQueryService;
import com.dynamicmart.order_service.service.InternalApiGuard;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/internal")
public class OrderInternalController {

    private final OrderQueryService queryService;
    private final InternalApiGuard internalApiGuard;

    public OrderInternalController(OrderQueryService queryService, InternalApiGuard internalApiGuard) {
        this.queryService = queryService;
        this.internalApiGuard = internalApiGuard;
    }

    @PostMapping("/review-eligibility")
    public ResponseEntity<ReviewEligibility> checkEligibility(
            @RequestHeader(name = InternalApiGuard.HEADER_NAME, required = false) String internalApiKey,
            @RequestBody ReviewEligibilityRequest request) {
        internalApiGuard.requireValid(internalApiKey);
        return ResponseEntity.ok(queryService.checkReviewEligibility(request.customerId(), request.orderItemId()));
    }

    public record ReviewEligibilityRequest(UUID customerId, UUID orderItemId) {
    }

    public record ReviewEligibility(
            boolean eligible,
            UUID orderId,
            UUID orderItemId,
            UUID customerId,
            UUID productId,
            UUID variantId,
            String reason
    ) {
    }
}
