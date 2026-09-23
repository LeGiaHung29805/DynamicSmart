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
@Table(name = "processed_events")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {
    @Id @Column(name = "event_id") private UUID eventId;
    @Column(name = "event_type", nullable = false, length = 120) private String eventType;
    @Column(nullable = false, length = 80) private String producer;
    @Column(name = "processed_at", nullable = false) private Instant processedAt;
    @Column(name = "correlation_id") private UUID correlationId;
}
