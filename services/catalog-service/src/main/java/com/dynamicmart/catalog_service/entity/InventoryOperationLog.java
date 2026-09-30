package com.dynamicmart.catalog_service.entity;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "inventory_operation_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryOperationLog {
    @Id
    @Column(name = "operation_key") private UUID operationKey;
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30) private InventoryOperationType operationType;
    @Column(name = "reservation_id") private UUID reservationId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_payload", nullable = false, columnDefinition = "jsonb") private JsonNode resultPayload;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public InventoryOperationLog(UUID operationKey, InventoryOperationType operationType,
                                 UUID reservationId, JsonNode resultPayload) {
        this.operationKey = operationKey;
        this.operationType = operationType;
        this.reservationId = reservationId;
        this.resultPayload = resultPayload;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
