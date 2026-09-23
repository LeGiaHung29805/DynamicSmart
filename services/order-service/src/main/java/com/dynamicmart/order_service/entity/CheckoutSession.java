package com.dynamicmart.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "checkout_sessions")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckoutSession {
    @Id private UUID id;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private CheckoutSource source;
    @Column(name = "cart_id") private UUID cartId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "selection_fingerprint", nullable = false, length = 64, columnDefinition = "char(64)")
    private String selectionFingerprint;
    @Column(name = "address_id") private UUID addressId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private CheckoutStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_timing", length = 20) private PaymentTiming paymentTiming;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20) private PaymentMethod paymentMethod;
    @Column(name = "items_list_subtotal_vnd") private Long itemsListSubtotalVnd;
    @Column(name = "direct_sale_discount_vnd") private Long directSaleDiscountVnd;
    @Column(name = "items_subtotal_vnd") private Long itemsSubtotalVnd;
    @Column(name = "product_discount_vnd") private Long productDiscountVnd;
    @Column(name = "order_discount_vnd") private Long orderDiscountVnd;
    @Column(name = "shipping_fee_vnd") private Long shippingFeeVnd;
    @Column(name = "shipping_discount_vnd") private Long shippingDiscountVnd;
    @Column(name = "final_total_vnd") private Long finalTotalVnd;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "completed_order_id") private UUID completedOrderId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    public static CheckoutSession create(
            UUID id,
            UUID customerId,
            CheckoutSource source,
            UUID cartId,
            String selectionFingerprint,
            Instant expiresAt,
            Instant now) {
        CheckoutSession session = new CheckoutSession();
        session.id = id;
        session.customerId = customerId;
        session.source = source;
        session.cartId = cartId;
        session.selectionFingerprint = selectionFingerprint;
        session.status = CheckoutStatus.ACTIVE;
        session.expiresAt = expiresAt;
        session.createdAt = now;
        session.updatedAt = now;
        return session;
    }
}
