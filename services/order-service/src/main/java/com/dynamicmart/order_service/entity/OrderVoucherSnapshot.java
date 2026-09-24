package com.dynamicmart.order_service.entity;

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
@Table(name = "order_voucher_snapshots")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderVoucherSnapshot {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "voucher_id") private UUID voucherId;
    @Column(name = "voucher_code", nullable = false, length = 80) private String voucherCode;
    @Column(nullable = false, length = 30) private String scope;
    @Column(name = "discount_method", nullable = false, length = 20) private String discountMethod;
    @Column(name = "discount_value") private Long discountValue;
    @Column(name = "eligible_subtotal_vnd", nullable = false) private long eligibleSubtotalVnd;
    @Column(name = "discount_amount_vnd", nullable = false) private long discountAmountVnd;
    @Column(name = "shipping_discount_vnd", nullable = false) private long shippingDiscountVnd;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public static OrderVoucherSnapshot create(
            UUID id,
            UUID orderId,
            UUID voucherId,
            String voucherCode,
            String scope,
            String discountMethod,
            Long discountValue,
            long eligibleSubtotalVnd,
            long discountAmountVnd,
            long shippingDiscountVnd,
            Instant now) {
        OrderVoucherSnapshot voucher = new OrderVoucherSnapshot();
        voucher.id = id;
        voucher.orderId = orderId;
        voucher.voucherId = voucherId;
        voucher.voucherCode = voucherCode;
        voucher.scope = scope;
        voucher.discountMethod = discountMethod;
        voucher.discountValue = discountValue;
        voucher.eligibleSubtotalVnd = eligibleSubtotalVnd;
        voucher.discountAmountVnd = discountAmountVnd;
        voucher.shippingDiscountVnd = shippingDiscountVnd;
        voucher.createdAt = now;
        return voucher;
    }
}
