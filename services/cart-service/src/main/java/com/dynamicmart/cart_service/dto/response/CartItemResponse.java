package com.dynamicmart.cart_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CartItemResponse(UUID id, UUID productId, UUID variantId, String productName,
                               String variantName, String imageUrl, int quantity, long version,
                               boolean selected, boolean purchasable, int availableQuantity,
                               String unavailableReason, long listPriceVnd, long salePriceVnd,
                               Instant updatedAt) { }
