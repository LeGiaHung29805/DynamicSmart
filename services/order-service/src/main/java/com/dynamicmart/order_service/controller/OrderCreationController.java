package com.dynamicmart.order_service.controller;

import com.dynamicmart.order_service.config.CurrentCustomer;
import com.dynamicmart.order_service.dto.response.CreateOrderResponse;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.service.OrderCreationOrchestrator;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout/sessions")
public class OrderCreationController {
    private final OrderCreationOrchestrator orderCreation;
    private final CurrentCustomer currentCustomer;

    public OrderCreationController(OrderCreationOrchestrator orderCreation, CurrentCustomer currentCustomer) {
        this.orderCreation = orderCreation;
        this.currentCustomer = currentCustomer;
    }

    @PostMapping("/{sessionId}/orders")
    public ResponseEntity<CreateOrderResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String rawIdempotencyKey) {
        UUID idempotencyKey = parseIdempotencyKey(rawIdempotencyKey);
        var result = orderCreation.create(currentCustomer.idFrom(jwt), sessionId, idempotencyKey);
        CreateOrderResponse response = new CreateOrderResponse(
                result.orderId(), result.orderNumber(), result.status(), result.sagaId(),
                result.paymentId(), result.paymentDueAt(), result.replay());
        return ResponseEntity.status(result.replay() ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }

    private UUID parseIdempotencyKey(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new OrderException(
                    HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", "Header Idempotency-Key là bắt buộc.");
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            throw new OrderException(
                    HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key phải là UUID hợp lệ.");
        }
    }
}
