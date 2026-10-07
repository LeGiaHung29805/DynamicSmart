package com.dynamicmart.engagement_service.controller;

import com.dynamicmart.engagement_service.dto.event.EventEnvelope;
import com.dynamicmart.engagement_service.dto.event.OrderCancelledPayload;
import com.dynamicmart.engagement_service.dto.event.OrderCompletedPayload;
import com.dynamicmart.engagement_service.dto.event.PaymentSucceededPayload;
import com.dynamicmart.engagement_service.dto.response.ApiResponse;
import com.dynamicmart.engagement_service.service.InternalApiGuard;
import com.dynamicmart.engagement_service.service.ReportingService;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/engagement/internal/events")
public class InternalEventController {
    private final ReportingService reporting;
    private final InternalApiGuard internalApiGuard;

    public InternalEventController(ReportingService reporting, InternalApiGuard internalApiGuard) {
        this.reporting = reporting;
        this.internalApiGuard = internalApiGuard;
    }

    @PostMapping("/order-completed")
    public ApiResponse<Boolean> orderCompleted(
            @RequestHeader(name = "X-Internal-Api-Key", required = false) String internalApiKey,
            @RequestBody EventEnvelope<OrderCompletedPayload> event) {
        internalApiGuard.requireValid(internalApiKey);
        return ApiResponse.of(reporting.processOrderCompleted(event));
    }

    @PostMapping("/payment-succeeded")
    public ApiResponse<Boolean> paymentSucceeded(
            @RequestHeader(name = "X-Internal-Api-Key", required = false) String internalApiKey,
            @RequestBody EventEnvelope<PaymentSucceededPayload> event) {
        internalApiGuard.requireValid(internalApiKey);
        return ApiResponse.of(reporting.processPaymentSucceeded(event));
    }

    @PostMapping("/order-cancelled")
    public ApiResponse<Boolean> orderCancelled(
            @RequestHeader(name = "X-Internal-Api-Key", required = false) String internalApiKey,
            @RequestBody EventEnvelope<OrderCancelledPayload> event) {
        internalApiGuard.requireValid(internalApiKey);
        return ApiResponse.of(reporting.processOrderCancelled(event));
    }
}
