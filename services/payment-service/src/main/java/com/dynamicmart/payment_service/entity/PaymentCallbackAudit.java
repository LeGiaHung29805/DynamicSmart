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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payment_callback_audits")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentCallbackAudit {
    @Id private UUID id;
    @Column(name = "payment_id") private UUID paymentId;
    @Column(nullable = false, length = 30) private String provider;
    @Column(name = "provider_transaction_ref", length = 100) private String providerTransactionRef;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb") private String rawPayload;
    @Column(name = "checksum_valid", nullable = false) private boolean checksumValid;
    @Column(name = "amount_valid", nullable = false) private boolean amountValid;
    @Column(name = "processed_result", nullable = false, length = 30) private String processedResult;
    @Column(name = "received_at", nullable = false) private Instant receivedAt;
}
