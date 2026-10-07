package com.dynamicmart.engagement_service.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID orderItemId,
        UUID orderId,
        UUID customerId,
        UUID productId,
        UUID variantId,
        short rating,
        String content,
        String status,
        String hiddenReason,
        List<String> imageUrls,
        Instant createdAt,
        Instant updatedAt
) {
}
