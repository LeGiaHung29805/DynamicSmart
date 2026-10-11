package com.dynamicmart.notification_service.messaging.consumer;

import com.dynamicmart.notification_service.dto.event.EventEnvelope;
import com.dynamicmart.notification_service.dto.event.OrderCancelledPayload;
import com.dynamicmart.notification_service.dto.event.OrderCompletedPayload;
import com.dynamicmart.notification_service.dto.event.PaymentSucceededPayload;
import com.dynamicmart.notification_service.service.NotificationEventService;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class NotificationEventConsumer {
    @Bean
    Consumer<EventEnvelope<JsonNode>> notificationEvents(
            NotificationEventService service, ObjectMapper objectMapper) {
        return event -> {
            if (event == null || event.eventType() == null) return;
            switch (event.eventType()) {
                case "OrderCompleted" ->
                        service.processOrderCompleted(typed(event, OrderCompletedPayload.class, objectMapper));
                case "OrderCancelled" ->
                        service.processOrderCancelled(typed(event, OrderCancelledPayload.class, objectMapper));
                case "PaymentSucceeded" ->
                        service.processPaymentSucceeded(typed(event, PaymentSucceededPayload.class, objectMapper));
                default -> {
                    // Exchange dùng chung; notification-service chỉ xử lý sự kiện của mình.
                }
            }
        };
    }

    private <T> EventEnvelope<T> typed(
            EventEnvelope<JsonNode> event, Class<T> type, ObjectMapper objectMapper) {
        T payload = event.payload() == null ? null : objectMapper.treeToValue(event.payload(), type);
        return new EventEnvelope<>(
                event.eventId(), event.eventType(), event.eventVersion(), event.producer(),
                event.aggregateId(), event.occurredAt(), event.correlationId(), payload);
    }
}