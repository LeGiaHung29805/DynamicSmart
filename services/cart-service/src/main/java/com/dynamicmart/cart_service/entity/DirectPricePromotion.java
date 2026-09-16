package com.dynamicmart.cart_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "direct_price_promotions")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DirectPricePromotion {
    @Id private UUID id;
    @Column(nullable = false, length = 200) private String name;
    private String description;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "discount_method", nullable = false, length = 20) private String discountMethod;
    @Column(name = "fixed_discount_vnd") private Long fixedDiscountVnd;
    @Column(name = "discount_rate_bps") private Integer discountRateBps;
    @Column(name = "max_discount_vnd") private Long maxDiscountVnd;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at", nullable = false) private Instant endsAt;
    @Column(name = "created_by", nullable = false) private UUID createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
