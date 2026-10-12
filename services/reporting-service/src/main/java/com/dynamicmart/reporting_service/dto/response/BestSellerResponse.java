package com.dynamicmart.reporting_service.dto.response;

import java.util.UUID;

public record BestSellerResponse(
        UUID productId,
        UUID variantId,
        long quantitySold,
        long grossSalesVnd,
        long netItemSalesVnd
) {
    public BestSellerResponse(UUID productId, long quantitySold,
                              long grossSalesVnd, long netItemSalesVnd) {
        this(productId, null, quantitySold, grossSalesVnd, netItemSalesVnd);
    }
}
