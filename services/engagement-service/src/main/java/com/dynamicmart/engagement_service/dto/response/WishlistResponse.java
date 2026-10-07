package com.dynamicmart.engagement_service.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WishlistResponse(UUID id, UUID customerId, List<WishlistItemResponse> items, Instant updatedAt) {
}
