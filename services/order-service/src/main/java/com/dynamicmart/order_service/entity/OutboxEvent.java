package com.dynamicmart.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {
    @Id private UUID id;
    @Column(name = "aggregate_type", nullable = false, length = 80) private String aggregateType;
    @Column(name = "aggregate_id", nullable = false) private UUID aggregateId;
    @Column(name = "event_type", nullable = false, length = 120) private String eventType;
    @Column(name = "event_version", nullable = false) private int eventVersion;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb") private String payload;
    @Column(name = "correlation_id") private UUID correlationId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private OutboxEventStatus status;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "available_at", nullable = false) private Instant availableAt;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public static OutboxEvent pending(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            int eventVersion,
            String payload,
            UUID correlationId,
            Instant now) {
        OutboxEvent event = new OutboxEvent();
        event.id = id;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.eventVersion = eventVersion;
        event.payload = payload;
        event.correlationId = correlationId;
        event.status = OutboxEventStatus.PENDING;
        event.attemptCount = 0;
        event.availableAt = now;
        event.createdAt = now;
        return event;
    }
}
