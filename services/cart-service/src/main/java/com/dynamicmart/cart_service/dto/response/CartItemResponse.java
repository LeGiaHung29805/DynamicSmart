package com.dynamicmart.cart_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CartItemResponse(UUID id, UUID productId, UUID variantId, int quantity, long version,
                               boolean selected, Instant updatedAt) { }
