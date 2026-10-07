package com.dynamicmart.engagement_service.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductQuestionResponse(
        UUID id,
        UUID productId,
        UUID customerId,
        String content,
        String status,
        String hiddenReason,
        List<ProductAnswerResponse> answers,
        Instant createdAt,
        Instant updatedAt
) {
}
