package com.dynamicmart.order_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PurchasedItemResponse(
        UUID orderId,
        String orderNumber,
        UUID orderItemId,
        UUID productId,
        UUID variantId,
        String productName,
        String variantName,
        String imageUrl,
        long unitPriceVnd,
        int quantity,
        Instant completedAt
) {
}
