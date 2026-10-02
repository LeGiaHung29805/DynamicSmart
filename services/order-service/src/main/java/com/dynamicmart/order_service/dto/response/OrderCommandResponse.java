package com.dynamicmart.order_service.dto.response;

import com.dynamicmart.order_service.entity.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCommandResponse(
        UUID orderId,
        OrderStatus status,
        List<OrderStatus> appliedTransitions,
        Instant updatedAt,
        boolean replay) {
}
