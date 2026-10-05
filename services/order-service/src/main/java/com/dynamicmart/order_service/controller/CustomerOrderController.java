package com.dynamicmart.order_service.controller;

import com.dynamicmart.order_service.config.CurrentCustomer;
import com.dynamicmart.order_service.dto.response.CustomerOrderResponse;
import com.dynamicmart.order_service.service.CustomerOrderService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class CustomerOrderController {
    private final CustomerOrderService orders;
    private final CurrentCustomer currentCustomer;

    public CustomerOrderController(CustomerOrderService orders, CurrentCustomer currentCustomer) {
        this.orders = orders;
        this.currentCustomer = currentCustomer;
    }

    @GetMapping("/{orderId}")
    public CustomerOrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return orders.get(orderId, currentCustomer.idFrom(jwt));
    }

    @PostMapping("/{orderId}/confirm-received")
    public CustomerOrderResponse confirmReceived(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return orders.confirmReceived(orderId, currentCustomer.idFrom(jwt));
    }
}
