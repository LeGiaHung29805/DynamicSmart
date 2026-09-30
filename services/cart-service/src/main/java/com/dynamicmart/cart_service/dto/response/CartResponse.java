package com.dynamicmart.cart_service.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(UUID id, UUID customerId, List<CartItemResponse> items, Instant updatedAt) { }
