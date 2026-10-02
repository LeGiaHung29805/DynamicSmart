package com.dynamicmart.order_service.dto.response;

import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPageResponse(
        List<OrderSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort) {

    public record OrderSummary(
            UUID orderId,
            String orderNumber,
            UUID customerId,
            OrderStatus status,
            PaymentTiming paymentTiming,
            PaymentMethod paymentMethod,
            long finalTotalVnd,
            String currency,
            Instant paymentDueAt,
            Instant createdAt,
            Instant updatedAt) {
    }
}
