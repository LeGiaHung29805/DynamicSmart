package com.dynamicmart.payment_service.api;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID orderId, long amountVnd, String timing, String method, String status,
                              String redirectUrl, Instant expiresAt, Instant paidAt, String codReceiptNo,
                              UUID codConfirmedBy, Instant codConfirmedAt) { }
