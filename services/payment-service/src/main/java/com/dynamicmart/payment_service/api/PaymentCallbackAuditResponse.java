package com.dynamicmart.payment_service.api;

import java.time.Instant;
import java.util.UUID;

public record PaymentCallbackAuditResponse(UUID id, String providerReference, boolean checksumValid, boolean amountValid,
                                           String processedResult, Instant receivedAt) { }
