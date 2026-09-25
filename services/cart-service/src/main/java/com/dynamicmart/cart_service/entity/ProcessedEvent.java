package com.dynamicmart.cart_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {
    @Id @Column(name = "event_id") private UUID eventId;
    @Column(name = "event_type", nullable = false, length = 120) private String eventType;
    @Column(nullable = false, length = 80) private String producer;
    @Column(name = "processed_at", nullable = false) private Instant processedAt;
    @Column(name = "correlation_id") private UUID correlationId;
    public ProcessedEvent(UUID eventId, String eventType, String producer, UUID correlationId, Instant now) {
        this.eventId = eventId; this.eventType = eventType; this.producer = producer;
        this.correlationId = correlationId; this.processedAt = now;
    }
}
