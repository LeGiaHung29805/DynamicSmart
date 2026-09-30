package com.dynamicmart.payment_service.entity;

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
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Payment {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(nullable = false, length = 20) private String timing;
    @Column(nullable = false, length = 20) private String method;
    @Column(name = "amount_vnd", nullable = false) private long amountVnd;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "failed_at") private Instant failedAt;
    @Column(name = "provider_transaction_ref", length = 100) private String providerTransactionRef;
    @Column(name = "correlation_id", nullable = false) private UUID correlationId;
    @Column(name = "cod_receipt_no", length = 100) private String codReceiptNo;
    @Column(name = "cod_confirmed_by") private UUID codConfirmedBy;
    @Column(name = "cod_confirmed_at") private Instant codConfirmedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
