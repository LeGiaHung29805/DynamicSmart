package com.dynamicmart.payment_service.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/** Weight and dimensions are a server-validated Catalog snapshot supplied by order-service. */
public record ShippingItemRequest(@NotNull UUID variantId, @NotNull @Positive Integer quantity,
                                  @NotNull @Positive Integer weightGrams, @NotNull @Positive Integer lengthCm,
                                  @NotNull @Positive Integer widthCm, @NotNull @Positive Integer heightCm) { }
