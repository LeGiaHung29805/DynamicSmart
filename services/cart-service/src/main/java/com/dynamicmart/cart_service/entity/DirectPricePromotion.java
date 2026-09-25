package com.dynamicmart.cart_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;
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
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "direct_price_promotion_variants", joinColumns = @JoinColumn(name = "promotion_id"))
    @Column(name = "variant_id", nullable = false)
    private Set<UUID> variantIds = new LinkedHashSet<>();

    public DirectPricePromotion(String name, String description, String discountMethod, Long fixedDiscountVnd,
                                Integer discountRateBps, Long maxDiscountVnd, Instant startsAt, Instant endsAt,
                                UUID createdBy, Set<UUID> variantIds, Instant now) {
        this.id = UUID.randomUUID(); this.name = name; this.description = description; this.status = "DRAFT";
        this.discountMethod = discountMethod; this.fixedDiscountVnd = fixedDiscountVnd; this.discountRateBps = discountRateBps;
        this.maxDiscountVnd = maxDiscountVnd; this.startsAt = startsAt; this.endsAt = endsAt; this.createdBy = createdBy;
        this.variantIds = new LinkedHashSet<>(variantIds);
        this.createdAt = now; this.updatedAt = now;
    }

    public void update(String name, String description, String discountMethod, Long fixedDiscountVnd,
                       Integer discountRateBps, Long maxDiscountVnd, Instant startsAt, Instant endsAt,
                       Set<UUID> variantIds, Instant now) {
        this.name = name; this.description = description; this.discountMethod = discountMethod;
        this.fixedDiscountVnd = fixedDiscountVnd; this.discountRateBps = discountRateBps;
        this.maxDiscountVnd = maxDiscountVnd; this.startsAt = startsAt; this.endsAt = endsAt;
        this.variantIds.clear(); this.variantIds.addAll(variantIds); this.updatedAt = now;
    }

    public void changeStatus(String status, Instant now) { this.status = status; this.updatedAt = now; }

    public long discount(long listPrice) {
        long value = "FIXED_AMOUNT".equals(discountMethod) ? fixedDiscountVnd : Math.multiplyExact(listPrice, discountRateBps) / 10_000;
        if (maxDiscountVnd != null) value = Math.min(value, maxDiscountVnd);
        return Math.min(Math.max(value, 0), listPrice);
    }
}
