package com.dynamicmart.engagement_service.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddWishlistItemRequest(@NotNull UUID productId) {
}
