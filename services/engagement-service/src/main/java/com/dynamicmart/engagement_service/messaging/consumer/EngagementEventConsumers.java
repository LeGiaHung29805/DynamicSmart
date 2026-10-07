package com.dynamicmart.engagement_service.messaging.consumer;

import com.dynamicmart.engagement_service.dto.event.EventEnvelope;
import com.dynamicmart.engagement_service.dto.event.OrderCancelledPayload;
import com.dynamicmart.engagement_service.dto.event.OrderCompletedPayload;
import com.dynamicmart.engagement_service.dto.event.PaymentSucceededPayload;
import com.dynamicmart.engagement_service.service.ReportingService;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class EngagementEventConsumers {
    @Bean
    Consumer<EventEnvelope<JsonNode>> engagementEvents(ReportingService reporting, ObjectMapper objectMapper) {
        return event -> {
            if (event == null || event.eventType() == null) {
                return;
            }
            switch (event.eventType()) {
                case "OrderCompleted" ->
                        reporting.processOrderCompleted(typed(event, OrderCompletedPayload.class, objectMapper));
                case "OrderCancelled" ->
                        reporting.processOrderCancelled(typed(event, OrderCancelledPayload.class, objectMapper));
                case "PaymentSucceeded" ->
                        reporting.processPaymentSucceeded(typed(event, PaymentSucceededPayload.class, objectMapper));
                default -> {
                    // Other events share dynamicmart.events but are owned by different services.
                }
            }
        };
    }

    private <T> EventEnvelope<T> typed(EventEnvelope<JsonNode> event, Class<T> payloadType,
                                       ObjectMapper objectMapper) {
        T payload = event.payload() == null ? null : objectMapper.treeToValue(event.payload(), payloadType);
        return new EventEnvelope<>(event.eventId(), event.eventType(), event.eventVersion(), event.producer(),
                event.aggregateId(), event.occurredAt(), event.correlationId(), payload);
    }
}
