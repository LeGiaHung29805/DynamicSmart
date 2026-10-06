package com.dynamicmart.order_service.controller;

import com.dynamicmart.order_service.config.CurrentCustomer;
import com.dynamicmart.order_service.dto.response.OrderCommandResponse;
import com.dynamicmart.order_service.service.OrderLifecycleCommandService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderLifecycleController {
    private final OrderLifecycleCommandService commands;
    private final CurrentCustomer currentCustomer;

    public OrderLifecycleController(OrderLifecycleCommandService commands, CurrentCustomer currentCustomer) {
        this.commands = commands;
        this.currentCustomer = currentCustomer;
    }

    @PostMapping("/{orderId}/received")
    public OrderCommandResponse confirmReceived(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId,
            @RequestHeader("Idempotency-Key") UUID operationKey) {
        return commands.customerConfirmReceived(currentCustomer.idFrom(jwt), orderId, operationKey);
    }

    @PostMapping("/admin/{orderId}/pack")
    public OrderCommandResponse pack(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId,
            @RequestHeader("Idempotency-Key") UUID operationKey) {
        return commands.adminPack(currentCustomer.idFrom(jwt), orderId, operationKey);
    }

    @PostMapping("/admin/{orderId}/ship")
    public OrderCommandResponse ship(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId,
            @RequestHeader("Idempotency-Key") UUID operationKey) {
        return commands.adminShip(currentCustomer.idFrom(jwt), orderId, operationKey);
    }

    @PostMapping("/admin/{orderId}/handover")
    public OrderCommandResponse handover(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId,
            @RequestHeader("Idempotency-Key") UUID operationKey) {
        return commands.adminHandover(currentCustomer.idFrom(jwt), orderId, operationKey);
    }
}
