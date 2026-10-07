package com.dynamicmart.engagement_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
        @NotBlank @Size(max = 2000) String content
) {
}
