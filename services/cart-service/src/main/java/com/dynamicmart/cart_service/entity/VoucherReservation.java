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
}
