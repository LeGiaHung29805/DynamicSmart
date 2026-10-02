package com.dynamicmart.order_service.dto.response;

import java.util.List;
import java.util.UUID;

public record OrderTimelineResponse(
        UUID orderId,
        List<OrderDetailResponse.TimelineEntry> timeline) {
}
