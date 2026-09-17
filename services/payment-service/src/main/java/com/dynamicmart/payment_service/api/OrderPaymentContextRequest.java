package com.dynamicmart.payment_service.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/** Trusted internal contract from order-service; never accept this payload directly from a browser. */
public record OrderPaymentContextRequest(
        @NotNull UUID orderId, @NotNull UUID customerId, @NotNull @Positive Long amountVnd,
        @NotNull PaymentTiming timing, @NotNull PaymentMethod method, UUID correlationId) {
    public enum PaymentTiming { PREPAID, POSTPAID }
    public enum PaymentMethod { VNPAY, COD }
}
