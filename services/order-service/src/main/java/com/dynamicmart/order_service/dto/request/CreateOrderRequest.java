package com.dynamicmart.order_service.dto.request;

import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Browser compatibility contract; all prices and benefits are reloaded server-side. */
public record CreateOrderRequest(
        @NotNull UUID cartId,
        @NotNull UUID addressId,
        UUID merchandiseVoucherId,
        UUID shippingVoucherId,
        UUID quoteId,
        @NotNull PaymentTiming paymentTiming,
        @NotNull PaymentMethod paymentMethod) {
}
