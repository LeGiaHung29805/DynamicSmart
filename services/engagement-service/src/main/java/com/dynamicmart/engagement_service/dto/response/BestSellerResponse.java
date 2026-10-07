package com.dynamicmart.engagement_service.dto.response;

import java.util.UUID;

public record BestSellerResponse(
        UUID productId,
        UUID variantId,
        long quantitySold,
        long grossSalesVnd,
        long netItemSalesVnd
) {
}
