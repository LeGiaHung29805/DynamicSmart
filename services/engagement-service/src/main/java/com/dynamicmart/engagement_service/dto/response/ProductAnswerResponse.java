package com.dynamicmart.engagement_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProductAnswerResponse(
        UUID id,
        UUID questionId,
        UUID adminId,
        String content,
        Instant createdAt
) {
}
