package com.dynamicmart.notification_service.messaging.consumer;

import com.dynamicmart.notification_service.dto.event.EventEnvelope;
import com.dynamicmart.notification_service.dto.event.OrderCancelledPayload;
import com.dynamicmart.notification_service.dto.event.OrderCompletedPayload;
import com.dynamicmart.notification_service.dto.event.PaymentSucceededPayload;
import com.dynamicmart.notification_service.service.NotificationEventService;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class NotificationEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    @Bean
    Consumer<EventEnvelope<Object>> notificationEvents(
            NotificationEventService service, ObjectMapper objectMapper) {
        return event -> {
            if (event == null || event.eventType() == null) return;
            try {
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
            } catch (Exception e) {
                log.error("Lỗi khi xử lý sự kiện {} (eventId: {}): {}",
                        event.eventType(), event.eventId(), e.getMessage(), e);
                throw e;
            }
        };
    }

    private <T> EventEnvelope<T> typed(
            EventEnvelope<?> event, Class<T> type, ObjectMapper objectMapper) {
        T payload = convertPayload(event.payload(), type, objectMapper);
        return new EventEnvelope<>(
                event.eventId(), event.eventType(), event.eventVersion(), event.producer(),
                event.aggregateId(), event.occurredAt(), event.correlationId(), payload);
    }

    @SuppressWarnings("unchecked")
    private <T> T convertPayload(Object payload, Class<T> type, ObjectMapper objectMapper) {
        if (payload == null) {
            return null;
        }
        if (type.isInstance(payload)) {
            return (T) payload;
        }
        if (payload instanceof JsonNode jsonNode) {
            return objectMapper.treeToValue(jsonNode, type);
        }
        return objectMapper.convertValue(payload, type);
    }
}