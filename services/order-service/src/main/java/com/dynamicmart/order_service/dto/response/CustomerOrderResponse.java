package com.dynamicmart.order_service.dto.response;

import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import java.time.Instant;
import java.util.UUID;

public record CustomerOrderResponse(
        UUID id,
        String orderNumber,
        OrderStatus status,
        PaymentTiming paymentTiming,
        PaymentMethod paymentMethod,
        long finalTotalVnd,
        String currency,
        boolean canConfirmReceived,
        Instant shipmentDeliveredAt,
        Instant completedAt,
        Instant createdAt) {
}
