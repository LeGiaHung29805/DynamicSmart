package com.dynamicmart.payment_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PaymentAttemptResponse(UUID id, int attemptNo, String provider, String reference, long amountVnd,
                                     String status, Instant expiresAt, Instant createdAt) { }
