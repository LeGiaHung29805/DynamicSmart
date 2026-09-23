package com.dynamicmart.order_service.dto.request;

import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import java.util.UUID;

/** Updates only selections owned by checkout; it never accepts client-side item prices. */
public record UpdateCheckoutSessionRequest(
        UUID addressId,
        PaymentTiming paymentTiming,
        PaymentMethod paymentMethod) {
}
