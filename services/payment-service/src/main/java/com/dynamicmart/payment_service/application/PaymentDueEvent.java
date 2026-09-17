package com.dynamicmart.payment_service.application;

import java.util.UUID;

/** Event contract from order-service when a POSTPAID + VNPAY Order enters HANDOVER_PENDING. */
public record PaymentDueEvent(UUID eventId, UUID orderId, UUID correlationId) { }
