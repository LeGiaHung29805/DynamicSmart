package com.dynamicmart.engagement_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "daily_sales_metrics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailySalesMetric {
    @Id
    @Column(name = "metric_date")
    private LocalDate metricDate;
    @Column(name = "gross_item_sales_vnd", nullable = false)
    private long grossItemSalesVnd;
    @Column(name = "discount_value_vnd", nullable = false)
    private long discountValueVnd;
    @Column(name = "shipping_fee_vnd", nullable = false)
    private long shippingFeeVnd;
    @Column(name = "net_revenue_vnd", nullable = false)
    private long netRevenueVnd;
    @Column(name = "order_count", nullable = false)
    private int orderCount;
    @Column(name = "completed_order_count", nullable = false)
    private int completedOrderCount;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DailySalesMetric(LocalDate metricDate, Instant now) {
        this.metricDate = metricDate;
        this.updatedAt = now;
    }

    public void addCompletedOrder(long grossItemSalesVnd, long discountValueVnd,
                                  long shippingFeeVnd, long netRevenueVnd, Instant now) {
        this.grossItemSalesVnd += grossItemSalesVnd;
        this.discountValueVnd += discountValueVnd;
        this.shippingFeeVnd += shippingFeeVnd;
        this.netRevenueVnd += netRevenueVnd;
        this.orderCount += 1;
        this.completedOrderCount += 1;
        this.updatedAt = now;
    }
}
