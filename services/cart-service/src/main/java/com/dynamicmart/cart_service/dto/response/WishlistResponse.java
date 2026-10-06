package com.dynamicmart.cart_service.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WishlistResponse(List<Item> items) {
    public record Item(UUID productId, Instant createdAt) { }
}
