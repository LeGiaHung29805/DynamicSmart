package com.dynamicmart.engagement_service.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatConversationResponse(
        UUID id,
        UUID customerId,
        UUID assignedAdminId,
        String status,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt,
        List<ChatMessageResponse> messages
) {
}
