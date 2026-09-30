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
@Table(name = "voucher_reservations")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoucherReservation {
    @Id private UUID id;
    @Column(name = "voucher_id", nullable = false) private UUID voucherId;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "customer_voucher_id") private UUID customerVoucherId;
    @Column(name = "checkout_session_id", nullable = false) private UUID checkoutSessionId;
    @Column(name = "order_id") private UUID orderId;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "discount_amount_vnd", nullable = false) private long discountAmountVnd;
    @Column(name = "shipping_discount_vnd", nullable = false) private long shippingDiscountVnd;
    @Column(name = "reserved_until", nullable = false) private Instant reservedUntil;
    @Column(name = "consumed_at") private Instant consumedAt;
    @Column(name = "released_at") private Instant releasedAt;
    @Column(name = "release_reason", length = 80) private String releaseReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public VoucherReservation(UUID voucherId, UUID customerId, UUID customerVoucherId, UUID checkoutSessionId,
                              long discountAmountVnd, long shippingDiscountVnd, Instant reservedUntil, Instant now) {
        this.id = UUID.randomUUID(); this.voucherId = voucherId; this.customerId = customerId;
        this.customerVoucherId = customerVoucherId; this.checkoutSessionId = checkoutSessionId; this.status = "RESERVED";
        this.discountAmountVnd = discountAmountVnd; this.shippingDiscountVnd = shippingDiscountVnd;
        this.reservedUntil = reservedUntil; this.createdAt = now; this.updatedAt = now;
    }
    public void consume(UUID orderId, Instant now) { if ("RESERVED".equals(status)) { this.status = "CONSUMED"; this.orderId = orderId; this.consumedAt = now; this.updatedAt = now; } }
    public void release(String reason, Instant now) { if ("RESERVED".equals(status)) { this.status = "RELEASED"; this.releaseReason = reason; this.releasedAt = now; this.updatedAt = now; } }
    public void expire(Instant now) { if ("RESERVED".equals(status) && !now.isBefore(reservedUntil)) { this.status = "EXPIRED"; this.releasedAt = now; this.releaseReason = "RESERVATION_EXPIRED"; this.updatedAt = now; } }
}
