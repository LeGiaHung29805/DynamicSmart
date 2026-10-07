package com.dynamicmart.engagement_service.mapper;

import com.dynamicmart.engagement_service.dto.response.DailySalesMetricResponse;
import com.dynamicmart.engagement_service.entity.DailySalesMetric;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {
    public DailySalesMetricResponse toResponse(DailySalesMetric metric) {
        return new DailySalesMetricResponse(metric.getMetricDate(), metric.getGrossItemSalesVnd(),
                metric.getDiscountValueVnd(), metric.getShippingFeeVnd(), metric.getNetRevenueVnd(),
                metric.getOrderCount(), metric.getCompletedOrderCount(), metric.getUpdatedAt());
    }
}
