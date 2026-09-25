package com.dynamicmart.payment_service.api;

import java.time.Instant;
import java.util.UUID;

public record PaymentAttemptResponse(UUID id, int attemptNo, String provider, String reference, long amountVnd,
                                     String status, Instant expiresAt, Instant createdAt) { }
