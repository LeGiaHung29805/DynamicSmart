package com.dynamicmart.order_service.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class CheckoutDtos {
    private CheckoutDtos() { }
    public record CreateOrderRequest(@NotNull UUID addressId, UUID merchandiseVoucherId, UUID shippingVoucherId,
                                     @NotNull UUID quoteId, @NotBlank String paymentTiming,
                                     @NotBlank String paymentMethod) { }
    public record OrderResult(UUID orderId, String orderNumber, String status, UUID paymentId, String redirectUrl) { }
}
