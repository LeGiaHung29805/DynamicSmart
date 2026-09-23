package com.dynamicmart.order_service.dto.response;

import java.util.UUID;

public record CheckoutSessionItemResponse(
        UUID id,
        UUID productId,
        UUID variantId,
        String sku,
        String productName,
        String variantName,
        String imageUrl,
        long listPriceVnd,
        long directSaleDiscountVnd,
        long unitPriceVnd,
        int quantity,
        int weightGrams,
        Integer lengthCm,
        Integer widthCm,
        Integer heightCm) {
}
