package com.dynamicmart.engagement_service.dto.event;

import java.util.UUID;

public record PaymentSucceededPayload(
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        Long amountVnd,
        String paymentMethod
) {
}
