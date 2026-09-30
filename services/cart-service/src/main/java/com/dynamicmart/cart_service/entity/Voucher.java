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
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "voucher_products", joinColumns = @JoinColumn(name = "voucher_id"))
    @Column(name = "product_id", nullable = false)
    private Set<UUID> productIds = new LinkedHashSet<>();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "voucher_categories", joinColumns = @JoinColumn(name = "voucher_id"))
    @Column(name = "category_id", nullable = false)
    private Set<UUID> categoryIds = new LinkedHashSet<>();

    public Voucher(String code, String name, String description, String scope, String discountMethod,
                   Long fixedDiscountVnd, Integer discountRateBps, Long maxDiscountVnd, Long minimumOrderVnd,
                   Long minimumEligibleSubtotalVnd, Integer usageLimit, Integer usageLimitPerCustomer,
                   Instant startsAt, Instant endsAt, String distributionMode, boolean defaultVoucher,
                   Set<UUID> productIds, Set<UUID> categoryIds, UUID createdBy, Instant now) {
        this.id = UUID.randomUUID(); this.code = code; this.name = name; this.description = description;
        this.status = "DRAFT"; this.scope = scope; this.discountMethod = discountMethod;
        this.fixedDiscountVnd = fixedDiscountVnd; this.discountRateBps = discountRateBps;
        this.maxDiscountVnd = maxDiscountVnd; this.minimumOrderVnd = minimumOrderVnd;
        this.minimumEligibleSubtotalVnd = minimumEligibleSubtotalVnd; this.usageLimit = usageLimit;
        this.usageLimitPerCustomer = usageLimitPerCustomer; this.startsAt = startsAt; this.endsAt = endsAt;
        this.distributionMode = distributionMode; this.defaultVoucher = defaultVoucher;
        this.productIds = new LinkedHashSet<>(productIds); this.categoryIds = new LinkedHashSet<>(categoryIds);
        this.createdBy = createdBy; this.createdAt = now; this.updatedAt = now;
    }

    public void update(String name, String description, String scope, String discountMethod,
                       Long fixedDiscountVnd, Integer discountRateBps, Long maxDiscountVnd, Long minimumOrderVnd,
                       Long minimumEligibleSubtotalVnd, Integer usageLimit, Integer usageLimitPerCustomer,
                       Instant startsAt, Instant endsAt, String distributionMode, boolean defaultVoucher,
                       Set<UUID> productIds, Set<UUID> categoryIds, Instant now) {
        this.name = name; this.description = description; this.scope = scope; this.discountMethod = discountMethod;
        this.fixedDiscountVnd = fixedDiscountVnd; this.discountRateBps = discountRateBps;
        this.maxDiscountVnd = maxDiscountVnd; this.minimumOrderVnd = minimumOrderVnd;
        this.minimumEligibleSubtotalVnd = minimumEligibleSubtotalVnd; this.usageLimit = usageLimit;
        this.usageLimitPerCustomer = usageLimitPerCustomer; this.startsAt = startsAt; this.endsAt = endsAt;
        this.distributionMode = distributionMode; this.defaultVoucher = defaultVoucher;
        this.productIds.clear(); this.productIds.addAll(productIds);
        this.categoryIds.clear(); this.categoryIds.addAll(categoryIds); this.updatedAt = now;
    }

    public void changeStatus(String status, Instant now) { this.status = status; this.updatedAt = now; }

    public long discount(long eligibleSubtotal) {
        long value = "FIXED_AMOUNT".equals(discountMethod) ? fixedDiscountVnd : Math.multiplyExact(eligibleSubtotal, discountRateBps) / 10_000;
        if (maxDiscountVnd != null) value = Math.min(value, maxDiscountVnd);
        return Math.min(Math.max(value, 0), eligibleSubtotal);
    }

    public boolean activeAt(Instant now) { return "ACTIVE".equals(status) && !now.isBefore(startsAt) && now.isBefore(endsAt); }
    public void consume() { this.consumedCount++; this.updatedAt = Instant.now(); }
}
