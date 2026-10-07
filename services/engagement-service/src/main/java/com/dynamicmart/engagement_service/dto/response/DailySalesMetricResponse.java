package com.dynamicmart.engagement_service.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record DailySalesMetricResponse(
        LocalDate metricDate,
        long grossItemSalesVnd,
        long discountValueVnd,
        long shippingFeeVnd,
        long netRevenueVnd,
        int orderCount,
        int completedOrderCount,
        Instant updatedAt
) {
}
