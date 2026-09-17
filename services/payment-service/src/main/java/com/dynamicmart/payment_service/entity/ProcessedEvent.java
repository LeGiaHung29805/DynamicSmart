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
@Table(name = "processed_events")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PUBLIC)
public class ProcessedEvent {
    @Id private UUID eventId;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(nullable = false) private String producer;
    @Column(name = "processed_at", nullable = false) private Instant processedAt;
    @Column(name = "correlation_id") private UUID correlationId;
}
