package com.dynamicmart.payment_service.messaging.producer;

import java.util.Map;

/** Broker boundary kept separate so outbox state transitions can be tested without RabbitMQ. */
public interface PaymentEventSender {
    boolean send(String eventType, Map<String, Object> envelope);
}
