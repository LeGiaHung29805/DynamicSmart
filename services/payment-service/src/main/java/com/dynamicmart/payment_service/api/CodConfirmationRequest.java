package com.dynamicmart.payment_service.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CodConfirmationRequest(@NotNull @Positive Long collectedAmountVnd, @NotBlank String receiptNo, @NotNull UUID confirmedBy) { }
