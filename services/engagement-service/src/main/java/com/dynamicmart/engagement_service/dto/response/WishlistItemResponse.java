package com.dynamicmart.engagement_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record WishlistItemResponse(UUID id, UUID productId, Instant createdAt) {
}
