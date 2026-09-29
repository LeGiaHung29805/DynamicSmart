package com.dynamicmart.order_service.api;

import static com.dynamicmart.order_service.api.CheckoutDtos.*;

import com.dynamicmart.order_service.application.CheckoutOrderService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout/orders")
public class CheckoutOrderController {
    private final CheckoutOrderService service;
    public CheckoutOrderController(CheckoutOrderService service) { this.service = service; }
    @PostMapping
    public OrderResult create(@AuthenticationPrincipal Jwt jwt,
                              @RequestHeader("Idempotency-Key") UUID idempotencyKey,
                              @Valid @RequestBody CreateOrderRequest request) {
        return service.create(UUID.fromString(jwt.getSubject()), jwt.getTokenValue(), idempotencyKey, request);
    }
}
