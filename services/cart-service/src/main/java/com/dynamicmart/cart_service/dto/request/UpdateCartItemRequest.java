package com.dynamicmart.cart_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

public record UpdateCartItemRequest(@Positive @Max(99) Integer quantity, Boolean selected, long version) { }
