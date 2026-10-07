package com.dynamicmart.engagement_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateProductQuestionRequest(
        @NotNull UUID productId,
        @NotBlank @Size(max = 2000) String content
) {
}
