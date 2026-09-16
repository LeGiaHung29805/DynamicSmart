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
@Table(name = "vouchers")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Voucher {
    @Id private UUID id;
    @Column(nullable = false, length = 80) private String code;
    @Column(nullable = false, length = 200) private String name;
    private String description;
    @Column(nullable = false, length = 20) private String status;
    @Column(nullable = false, length = 30) private String scope;
    @Column(name = "discount_method", nullable = false, length = 20) private String discountMethod;
    @Column(name = "fixed_discount_vnd") private Long fixedDiscountVnd;
    @Column(name = "discount_rate_bps") private Integer discountRateBps;
    @Column(name = "max_discount_vnd") private Long maxDiscountVnd;
    @Column(name = "minimum_order_vnd") private Long minimumOrderVnd;
    @Column(name = "minimum_eligible_subtotal_vnd") private Long minimumEligibleSubtotalVnd;
    @Column(name = "usage_limit") private Integer usageLimit;
    @Column(name = "consumed_count", nullable = false) private int consumedCount;
    @Column(name = "usage_limit_per_customer") private Integer usageLimitPerCustomer;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at", nullable = false) private Instant endsAt;
    @Column(name = "distribution_mode", nullable = false, length = 30) private String distributionMode;
    @Column(name = "is_default", nullable = false) private boolean defaultVoucher;
    @Column(name = "created_by", nullable = false) private UUID createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
