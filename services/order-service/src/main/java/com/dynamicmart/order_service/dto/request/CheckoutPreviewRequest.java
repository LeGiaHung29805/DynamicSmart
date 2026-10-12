package com.dynamicmart.order_service.dto.request;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CheckoutPreviewRequest(
        UUID merchandiseVoucherId,
        UUID shippingVoucherId,
        @Size(max = 80, message = "merchandiseVoucherCode không được vượt quá 80 ký tự.") String merchandiseVoucherCode,
        @Size(max = 80, message = "shippingVoucherCode không được vượt quá 80 ký tự.") String shippingVoucherCode,
        @Size(max = 50, message = "serviceCode không được vượt quá 50 ký tự.") String serviceCode) {
    public CheckoutPreviewRequest(UUID merchandiseVoucherId, UUID shippingVoucherId, String serviceCode) {
        this(merchandiseVoucherId, shippingVoucherId, null, null, serviceCode);
    }
}
