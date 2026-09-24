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
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {
    @Id private UUID id;
    @Column(name = "aggregate_type", nullable = false, length = 80) private String aggregateType;
    @Column(name = "aggregate_id", nullable = false) private UUID aggregateId;
    @Column(name = "event_type", nullable = false, length = 120) private String eventType;
    @Column(name = "event_version", nullable = false) private int eventVersion;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb") private JsonNode payload;
    @Column(name = "correlation_id") private UUID correlationId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private OutboxStatus status;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "available_at", nullable = false) private Instant availableAt;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public OutboxEvent(UUID id, String aggregateType, UUID aggregateId, String eventType,
                       JsonNode payload, UUID correlationId) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventVersion = 1;
        this.payload = payload;
        this.correlationId = correlationId;
        this.status = OutboxStatus.PENDING;
        this.availableAt = Instant.now();
    }

    public void markPublished(Instant now) { status = OutboxStatus.PUBLISHED; publishedAt = now; }
    public void markFailed(Instant retryAt) { status = OutboxStatus.FAILED; attemptCount++; availableAt = retryAt; }
    public void scheduleRetry(Instant retryAt) { status = OutboxStatus.PENDING; availableAt = retryAt; }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (availableAt == null) availableAt = createdAt;
        if (status == null) status = OutboxStatus.PENDING;
    }
}
