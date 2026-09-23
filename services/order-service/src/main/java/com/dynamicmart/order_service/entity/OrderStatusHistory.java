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

@Entity
@Table(name = "order_status_history")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistory {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 40) private OrderStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 40) private OrderStatus toStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20) private OrderActorType actorType;
    @Column(name = "actor_id") private UUID actorId;
    @Column(length = 500) private String reason;
    @Column(name = "correlation_id") private UUID correlationId;
    @Column(name = "source_event_id") private UUID sourceEventId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
