package com.dynamicmart.catalog_service.dto.response;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record CatalogEventMessage(
        UUID eventId,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        int eventVersion,
        JsonNode payload,
        UUID correlationId,
        Instant occurredAt
) {
}
