package com.dynamicmart.engagement_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HideReviewRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
