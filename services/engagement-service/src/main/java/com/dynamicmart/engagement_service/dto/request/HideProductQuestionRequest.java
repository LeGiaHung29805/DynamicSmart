package com.dynamicmart.engagement_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HideProductQuestionRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
