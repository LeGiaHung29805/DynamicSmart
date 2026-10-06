package com.dynamicmart.order_service.controller;

import com.dynamicmart.order_service.config.CurrentCustomer;
import com.dynamicmart.order_service.dto.response.OrderDetailResponse;
import com.dynamicmart.order_service.dto.response.OrderPageResponse;
import com.dynamicmart.order_service.dto.response.OrderTimelineResponse;
import com.dynamicmart.order_service.service.OrderQueryService;
import com.dynamicmart.order_service.service.OrderQueryService.Query;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderQueryController {
    private final OrderQueryService queries;
    private final CurrentCustomer currentCustomer;

    public OrderQueryController(OrderQueryService queries, CurrentCustomer currentCustomer) {
        this.queries = queries;
        this.currentCustomer = currentCustomer;
    }

    @GetMapping
    public OrderPageResponse listCustomerOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return queries.listCustomerOrders(
                currentCustomer.idFrom(jwt), query(page, size, status, orderNumber, createdFrom, createdTo, sort));
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse getCustomerOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId) {
        return queries.getCustomerOrder(currentCustomer.idFrom(jwt), orderId);
    }

    @GetMapping("/{orderId}/timeline")
    public OrderTimelineResponse getCustomerTimeline(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId) {
        return queries.getCustomerTimeline(currentCustomer.idFrom(jwt), orderId);
    }

    @GetMapping("/admin")
    public OrderPageResponse listAdminOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return queries.listAdminOrders(
                query(page, size, status, orderNumber, createdFrom, createdTo, sort), customerId);
    }

    @GetMapping("/admin/{orderId}")
    public OrderDetailResponse getAdminOrder(@PathVariable UUID orderId) {
        return queries.getAdminOrder(orderId);
    }

    @GetMapping("/admin/{orderId}/timeline")
    public OrderTimelineResponse getAdminTimeline(@PathVariable UUID orderId) {
        return queries.getAdminTimeline(orderId);
    }

    private Query query(
            int page,
            int size,
            String status,
            String orderNumber,
            Instant createdFrom,
            Instant createdTo,
            String sort) {
        return new Query(page, size, status, orderNumber, createdFrom, createdTo, sort);
    }
}
