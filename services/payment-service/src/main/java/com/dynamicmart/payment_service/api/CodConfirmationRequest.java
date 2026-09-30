package com.dynamicmart.payment_service.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CodConfirmationRequest(@Positive long collectedAmountVnd, @NotBlank String receiptNo) { }
