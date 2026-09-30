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
@Table(name = "outbox_events")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PUBLIC)
public class OutboxEvent {
    @Id private UUID id;
    @Column(name = "aggregate_type", nullable = false) private String aggregateType;
    @Column(name = "aggregate_id", nullable = false) private UUID aggregateId;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(name = "event_version", nullable = false) private int eventVersion;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private String payload;
    @Column(name = "correlation_id") private UUID correlationId;
    @Column(nullable = false) private String status;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "available_at", nullable = false) private Instant availableAt;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
