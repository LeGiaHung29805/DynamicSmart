package com.dynamicmart.reporting_service.messaging.consumer;

import com.dynamicmart.reporting_service.dto.event.EventEnvelope;
import com.dynamicmart.reporting_service.dto.event.OrderCompletedPayload;
import com.dynamicmart.reporting_service.service.ReportingService;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class ReportingEventConsumer {
    @Bean
    Consumer<EventEnvelope<JsonNode>> reportingEvents(
            ReportingService reporting, ObjectMapper objectMapper) {
        return event -> {
            if (event == null || !"OrderCompleted".equals(event.eventType())) return;
            OrderCompletedPayload payload = event.payload() == null
                    ? null : objectMapper.treeToValue(event.payload(), OrderCompletedPayload.class);
            reporting.processOrderCompleted(new EventEnvelope<>(
                    event.eventId(), event.eventType(), event.eventVersion(), event.producer(),
                    event.aggregateId(), event.occurredAt(), event.correlationId(), payload));
        };
    }
}