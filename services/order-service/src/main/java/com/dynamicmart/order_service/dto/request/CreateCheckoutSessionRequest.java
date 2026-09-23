package com.dynamicmart.order_service.dto.request;

import com.dynamicmart.order_service.entity.CheckoutSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/**
 * The client identifies a selection only. Product snapshots and prices are always resolved
 * server-side through Cart/Catalog before a session is created.
 */
public record CreateCheckoutSessionRequest(
        @NotNull(message = "source là bắt buộc.") CheckoutSource source,
        UUID cartId,
        UUID variantId,
        @Positive(message = "quantity phải lớn hơn 0.") Integer quantity) {
}
