package com.dynamicmart.reporting_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.reporting_service.dto.event.EventEnvelope;
import com.dynamicmart.reporting_service.dto.event.OrderCompletedPayload;
import com.dynamicmart.reporting_service.mapper.ReportMapper;
import com.dynamicmart.reporting_service.repository.DailyProductMetricRepository;
import com.dynamicmart.reporting_service.repository.DailySalesMetricRepository;
import com.dynamicmart.reporting_service.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportingServiceTest {
    private final DailySalesMetricRepository dailySales =
            org.mockito.Mockito.mock(DailySalesMetricRepository.class);
    private final DailyProductMetricRepository dailyProducts =
            org.mockito.Mockito.mock(DailyProductMetricRepository.class);
    private final ProcessedEventRepository processedEvents =
            org.mockito.Mockito.mock(ProcessedEventRepository.class);
    private final ReportingService service =
            new ReportingService(dailySales, dailyProducts, processedEvents, new ReportMapper());

    @Test
    void duplicateOrderCompletedDoesNotChangeProjection() {
        UUID eventId = UUID.randomUUID();
        when(processedEvents.insertIfAbsent(org.mockito.ArgumentMatchers.eq(eventId),
                org.mockito.ArgumentMatchers.eq("OrderCompleted"),
                org.mockito.ArgumentMatchers.eq("order-service"),
                org.mockito.ArgumentMatchers.any(Instant.class),
                org.mockito.ArgumentMatchers.any(UUID.class))).thenReturn(0);
        EventEnvelope<OrderCompletedPayload> event = new EventEnvelope<>(
                eventId, "OrderCompleted", 1, "order-service", UUID.randomUUID(),
                Instant.now(), UUID.randomUUID(),
                new OrderCompletedPayload(
                        UUID.randomUUID(), UUID.randomUUID(), 100_000L,
                        100_000L, 0L, 0L, List.of()));

        assertThat(service.processOrderCompleted(event)).isFalse();
        verify(dailySales, never()).save(org.mockito.ArgumentMatchers.any());
        verify(dailyProducts, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deserializesOrderCompletedPayloadWithExtraFields() {
        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
        String json = "{\"orderId\":\"" + UUID.randomUUID() + "\",\"orderNumber\":\"DM-001\",\"customerId\":\"" + UUID.randomUUID() + "\"}";
        tools.jackson.databind.JsonNode node = mapper.readTree(json);
        OrderCompletedPayload payload = mapper.treeToValue(node, OrderCompletedPayload.class);
        assertThat(payload).isNotNull();
    }

    @Test
    void deserializesEventEnvelopeWithJsonNode() {
        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
        String json = "{\"eventId\":\"" + UUID.randomUUID() + "\",\"eventType\":\"OrderCompleted\",\"eventVersion\":1,\"producer\":\"order-service\",\"aggregateId\":\"" + UUID.randomUUID() + "\",\"occurredAt\":\"2026-10-11T10:00:00Z\",\"correlationId\":\"" + UUID.randomUUID() + "\",\"payload\":{\"orderId\":\"" + UUID.randomUUID() + "\",\"customerId\":\"" + UUID.randomUUID() + "\"}}";
        EventEnvelope<tools.jackson.databind.JsonNode> envelope = mapper.readValue(json, new tools.jackson.core.type.TypeReference<EventEnvelope<tools.jackson.databind.JsonNode>>() {});
        assertThat(envelope).isNotNull();
        assertThat(envelope.eventType()).isEqualTo("OrderCompleted");
        assertThat(envelope.payload()).isNotNull();
    }
}