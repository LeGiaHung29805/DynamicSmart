package com.dynamicmart.payment_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID orderId, long amountVnd, String timing, String method, String status,
                              String redirectUrl, Instant expiresAt, Instant paidAt,
                              UUID codConfirmedBy, Instant codConfirmedAt) { }
