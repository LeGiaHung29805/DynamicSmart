package com.dynamicmart.order_service.controller;

import com.dynamicmart.order_service.service.OrderQueryService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/internal")
public class OrderInternalController {

    private final OrderQueryService queryService;

    public OrderInternalController(OrderQueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping("/review-eligibility")
    public ResponseEntity<ReviewEligibility> checkEligibility(@RequestBody ReviewEligibilityRequest request) {
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
