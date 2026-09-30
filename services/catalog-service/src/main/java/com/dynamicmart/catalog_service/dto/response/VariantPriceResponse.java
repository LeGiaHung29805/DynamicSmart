package com.dynamicmart.catalog_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record VariantPriceResponse(
        long listPriceVnd,
        Long salePriceVnd,
        long directSaleDiscountVnd,
        Integer directSalePercent,
        Instant directSaleEndsAt,
        UUID directSalePromotionId
) {
    public static VariantPriceResponse listPrice(long listPriceVnd) {
        return new VariantPriceResponse(listPriceVnd, null, 0, null, null, null);
    }
}
