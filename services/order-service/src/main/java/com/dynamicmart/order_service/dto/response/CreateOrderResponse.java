package com.dynamicmart.order_service.dto.response;

import com.dynamicmart.order_service.entity.OrderStatus;
import java.util.UUID;

public record CreateOrderResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus status,
        String redirectUrl) {
}
