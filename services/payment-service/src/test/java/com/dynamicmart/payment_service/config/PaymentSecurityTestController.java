package com.dynamicmart.payment_service.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PaymentSecurityTestController {
    @GetMapping("/actuator/health") String health() { return "UP"; }
    @GetMapping("/api/v1/payments/methods") String methods() { return "methods"; }
    @GetMapping("/api/v1/payments/my-orders/00000000-0000-0000-0000-000000000001") String customer() { return "customer"; }
    @GetMapping("/api/v1/payments") String admin() { return "admin"; }
    @PostMapping("/api/v1/locations/sync") String sync() { return "sync"; }
    @PostMapping("/api/v1/payments/order-context") String internal() { return "internal"; }
}