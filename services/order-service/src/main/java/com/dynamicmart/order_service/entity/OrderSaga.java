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
@Table(name = "order_sagas")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderSaga {
    @Id private UUID id;
    @Column(name = "checkout_session_id", nullable = false) private UUID checkoutSessionId;
    @Column(name = "idempotency_key", nullable = false) private UUID idempotencyKey;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "request_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String requestHash;
    @Column(name = "order_id") private UUID orderId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40) private SagaStatus status;
    @Column(name = "current_step", nullable = false, length = 80) private String currentStep;
    @Column(name = "voucher_reservation_id") private UUID voucherReservationId;
    @Column(name = "inventory_reservation_id") private UUID inventoryReservationId;
    @Column(name = "correlation_id", nullable = false) private UUID correlationId;
    @Column(name = "last_error_code", length = 100) private String lastErrorCode;
    @Column(name = "last_error_message", length = 1000) private String lastErrorMessage;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Version @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "completed_at") private Instant completedAt;

    public static OrderSaga start(
            UUID id,
            UUID checkoutSessionId,
            UUID idempotencyKey,
            String requestHash,
            UUID correlationId,
            Instant now) {
        OrderSaga saga = new OrderSaga();
        saga.id = id;
        saga.checkoutSessionId = checkoutSessionId;
        saga.idempotencyKey = idempotencyKey;
        saga.requestHash = requestHash;
        saga.status = SagaStatus.STARTED;
        saga.currentStep = "ADMITTED";
        saga.correlationId = correlationId;
        saga.attemptCount = 0;
        saga.createdAt = now;
        saga.updatedAt = now;
        return saga;
    }

    public void markOrderCreated(UUID createdOrderId, Instant now) {
        orderId = createdOrderId;
        status = SagaStatus.ORDER_CREATED;
        currentStep = "ORDER_SNAPSHOTS_PERSISTED";
        lastErrorCode = null;
        lastErrorMessage = null;
        updatedAt = now;
    }
}
