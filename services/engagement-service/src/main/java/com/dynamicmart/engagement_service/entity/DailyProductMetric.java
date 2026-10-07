package com.dynamicmart.engagement_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "daily_product_metrics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyProductMetric {
    @EmbeddedId
    private DailyProductMetricId id;
    @Column(name = "quantity_sold", nullable = false)
    private int quantitySold;
    @Column(name = "gross_sales_vnd", nullable = false)
    private long grossSalesVnd;
    @Column(name = "net_item_sales_vnd", nullable = false)
    private long netItemSalesVnd;
    @Column(name = "review_count", nullable = false)
    private int reviewCount;
    @Column(name = "rating_sum", nullable = false)
    private int ratingSum;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DailyProductMetric(DailyProductMetricId id, Instant now) {
        this.id = id;
        this.updatedAt = now;
    }

    public void addSoldQuantity(int quantitySold, long grossSalesVnd, long netItemSalesVnd, Instant now) {
        this.quantitySold += quantitySold;
        this.grossSalesVnd += grossSalesVnd;
        this.netItemSalesVnd += netItemSalesVnd;
        this.updatedAt = now;
    }
}
