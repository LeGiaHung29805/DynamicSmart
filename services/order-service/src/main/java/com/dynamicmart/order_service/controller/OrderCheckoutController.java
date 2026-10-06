package com.dynamicmart.order_service.controller;

import com.dynamicmart.order_service.dto.request.CreateOrderRequest;
import com.dynamicmart.order_service.dto.response.CreateOrderResponse;
import com.dynamicmart.order_service.config.CurrentCustomer;
import com.dynamicmart.order_service.service.CreateOrderFacade;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout/orders")
public class OrderCheckoutController {
    private final CreateOrderFacade orders;
    private final CurrentCustomer currentCustomer;

    public OrderCheckoutController(CreateOrderFacade orders, CurrentCustomer currentCustomer) {
        this.orders = orders;
        this.currentCustomer = currentCustomer;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResponse response = orders.create(currentCustomer.idFrom(jwt), idempotencyKey, request);
        return ResponseEntity.status(response.replay() ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }
}
