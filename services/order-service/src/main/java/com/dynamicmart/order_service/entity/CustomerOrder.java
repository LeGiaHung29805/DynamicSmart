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
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerOrder {
    @Id private UUID id;
    @Column(name = "order_number", nullable = false, length = 32) private String orderNumber;
    @Column(name = "checkout_session_id", nullable = false) private UUID checkoutSessionId;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(nullable = false, length = 40) private String status;
    @Column(name = "payment_timing", nullable = false, length = 20) private String paymentTiming;
    @Column(name = "payment_method", nullable = false, length = 20) private String paymentMethod;
    @Column(name = "items_list_subtotal_vnd", nullable = false) private long itemsListSubtotalVnd;
    @Column(name = "direct_sale_discount_vnd", nullable = false) private long directSaleDiscountVnd;
    @Column(name = "items_subtotal_vnd", nullable = false) private long itemsSubtotalVnd;
    @Column(name = "product_discount_vnd", nullable = false) private long productDiscountVnd;
    @Column(name = "order_discount_vnd", nullable = false) private long orderDiscountVnd;
    @Column(name = "shipping_fee_vnd", nullable = false) private long shippingFeeVnd;
    @Column(name = "shipping_discount_vnd", nullable = false) private long shippingDiscountVnd;
    @Column(name = "final_total_vnd", nullable = false) private long finalTotalVnd;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "payment_due_at") private Instant paymentDueAt;
    @Column(name = "payment_succeeded_at") private Instant paymentSucceededAt;
    @Column(name = "shipment_delivered_at") private Instant shipmentDeliveredAt;
    @Column(name = "cancel_reason", length = 80) private String cancelReason;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "confirmed_at") private Instant confirmedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
