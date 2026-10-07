package com.dynamicmart.engagement_service.dto.event;

import java.util.UUID;

public record OrderCancelledPayload(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        String cancelReason
) {
}
