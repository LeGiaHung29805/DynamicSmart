package com.dynamicmart.payment_service.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.UUID;

/** Internal order-service contract. Browser shipping fees and warehouse/provider fields are never accepted. */
public record ShippingQuoteRequest(@NotNull UUID customerId, @NotNull Integer provinceId, @NotNull Integer wardId,
                                   @NotEmpty List<@Valid ShippingItemRequest> items,
                                   @NotNull @PositiveOrZero Long shippingDiscountVnd, String serviceCode) { }
