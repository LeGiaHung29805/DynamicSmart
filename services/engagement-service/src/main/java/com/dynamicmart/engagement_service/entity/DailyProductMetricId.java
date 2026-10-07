package com.dynamicmart.engagement_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class DailyProductMetricId implements Serializable {
    @Column(name = "metric_date")
    private LocalDate metricDate;
    @Column(name = "product_id")
    private UUID productId;
    @Column(name = "variant_id")
    private UUID variantId;
}
