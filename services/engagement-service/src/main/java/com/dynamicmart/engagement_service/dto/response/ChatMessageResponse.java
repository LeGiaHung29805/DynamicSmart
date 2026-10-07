package com.dynamicmart.engagement_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderRole,
        String content,
        UUID idempotencyKey,
        Instant readAt,
        Instant createdAt
) {
}
