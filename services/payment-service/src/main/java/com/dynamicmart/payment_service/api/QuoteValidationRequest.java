package com.dynamicmart.payment_service.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Order service submits its recomputed fingerprint before it snapshots a quote into an Order. */
public record QuoteValidationRequest(@NotNull UUID customerId, @NotBlank String requestFingerprint) { }
