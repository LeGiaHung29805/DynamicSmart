package com.dynamicmart.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "checkout_session_vouchers")
@Getter @Setter @NoArgsConstructor
public class CheckoutSessionVoucher {
    @Id private UUID id;
    @Column(name = "checkout_session_id", nullable = false) private UUID checkoutSessionId;
    @Column(name = "voucher_id", nullable = false) private UUID voucherId;
    @Column(name = "voucher_reservation_id") private UUID voucherReservationId;
    @Column(name = "voucher_code", nullable = false, length = 80) private String voucherCode;
    @Column(nullable = false, length = 30) private String scope;
    @Column(name = "discount_amount_vnd", nullable = false) private long discountAmountVnd;
    @Column(name = "shipping_discount_vnd", nullable = false) private long shippingDiscountVnd;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
