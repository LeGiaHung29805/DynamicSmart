package com.dynamicmart.engagement_service.dto.response;

import java.util.Map;
import java.util.UUID;

public record ReviewSummaryResponse(
        UUID productId,
        double averageRating,
        long totalReviews,
        Map<Integer, Long> ratingBreakdown
) {
}
