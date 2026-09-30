package com.dynamicmart.cart_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record AddCartItemRequest(@NotNull UUID productId, @NotNull UUID variantId, @Positive @Max(99) int quantity) { }
