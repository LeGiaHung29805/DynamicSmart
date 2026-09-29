package com.dynamicmart.order_service.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityTestController {
    @GetMapping("/actuator/health")
    String health() {
        return "UP";
    }

    @GetMapping("/api/v1/orders/current")
    String customer() {
        return "customer";
    }

    @GetMapping("/api/v1/orders/admin/current")
    String admin() {
        return "admin";
    }
}
