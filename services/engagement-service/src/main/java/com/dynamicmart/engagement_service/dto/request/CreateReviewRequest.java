package com.dynamicmart.engagement_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateReviewRequest(
        @NotNull UUID orderItemId,
        @Min(1) @Max(5) short rating,
        @Size(max = 2000) String content,
        @Size(max = 5) List<@Size(max = 1000) String> imageUrls
) {
}
