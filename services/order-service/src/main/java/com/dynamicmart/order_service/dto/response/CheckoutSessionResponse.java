package com.dynamicmart.order_service.dto.response;

import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.CheckoutStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CheckoutSessionResponse(
        UUID id,
        CheckoutSource source,
        UUID cartId,
        UUID addressId,
        CheckoutStatus status,
        PaymentTiming paymentTiming,
        PaymentMethod paymentMethod,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt,
        List<CheckoutSessionItemResponse> items) {
    public CheckoutSessionResponse {
        items = List.copyOf(items);
    }
}
