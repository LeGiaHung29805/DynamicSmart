package com.dynamicmart.order_service.dto.request;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CheckoutPreviewRequest(
        UUID merchandiseVoucherId,
        UUID shippingVoucherId,
        @Size(max = 50, message = "serviceCode không được vượt quá 50 ký tự.") String serviceCode) {
}
