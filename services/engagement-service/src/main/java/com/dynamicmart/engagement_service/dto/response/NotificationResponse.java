package com.dynamicmart.engagement_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String content,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
