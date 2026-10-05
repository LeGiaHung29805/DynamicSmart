package com.dynamicmart.payment_service.messaging.consumer;

import java.util.UUID;

/** Event contract from order-service when a POSTPAID online-payment Order enters HANDOVER_PENDING. */
public record PaymentDueEvent(UUID eventId, UUID orderId, UUID correlationId) { }
