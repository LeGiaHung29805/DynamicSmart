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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "order_operation_log")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderOperationLog {
    @Id @Column(name = "operation_key") private UUID operationKey;
    @Column(name = "operation_type", nullable = false, length = 50) private String operationType;
    @Column(name = "order_id") private UUID orderId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_payload", nullable = false, columnDefinition = "jsonb") private String resultPayload;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
