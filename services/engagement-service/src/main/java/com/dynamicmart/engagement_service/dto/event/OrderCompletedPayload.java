package com.dynamicmart.engagement_service.dto.event;

import java.util.List;
import java.util.UUID;

public record OrderCompletedPayload(
        UUID orderId,
        UUID customerId,
        Long finalTotalVnd,
        Long grossItemSalesVnd,
        Long discountValueVnd,
        Long shippingFeeVnd,
        List<OrderCompletedItemPayload> items
) {
    public record OrderCompletedItemPayload(
            UUID productId,
            UUID variantId,
            Integer quantity,
            Long grossSalesVnd,
            Long netItemSalesVnd
    ) {
    }
}
