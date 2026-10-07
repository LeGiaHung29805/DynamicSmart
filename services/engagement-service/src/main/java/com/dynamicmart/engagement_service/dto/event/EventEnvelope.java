package com.dynamicmart.engagement_service.dto.event;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        Integer eventVersion,
        String producer,
        UUID aggregateId,
        Instant occurredAt,
        UUID correlationId,
        T payload
) {
}
