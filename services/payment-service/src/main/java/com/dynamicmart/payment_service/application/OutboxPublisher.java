package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.entity.OutboxEvent;
import com.dynamicmart.payment_service.repository.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {
    private static final int MAX_ATTEMPTS = 10;
    private final OutboxEventRepository events;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository events, StreamBridge streamBridge, ObjectMapper objectMapper) {
        this.events = events;
        this.streamBridge = streamBridge;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:1000}")
    public void publishPending() {
        for (OutboxEvent event : events.findTop100ByStatusAndAvailableAtBeforeOrderByCreatedAtAsc("PENDING", Instant.now())) {
            publishOne(event.getId());
        }
    }

    public void publishOne(java.util.UUID eventId) {
        OutboxEvent event = events.findById(eventId).orElse(null);
        if (event == null || !"PENDING".equals(event.getStatus()) || event.getAvailableAt().isAfter(Instant.now())) return;
        try {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("eventId", event.getId()); envelope.put("eventType", event.getEventType()); envelope.put("eventVersion", event.getEventVersion());
            envelope.put("producer", "payment-service"); envelope.put("aggregateId", event.getAggregateId()); envelope.put("occurredAt", event.getCreatedAt());
            envelope.put("correlationId", event.getCorrelationId()); envelope.put("payload", objectMapper.readTree(event.getPayload()));
            boolean sent = streamBridge.send("paymentEvents-out-0", MessageBuilder.withPayload(envelope).setHeader("eventType", event.getEventType()).build());
            if (!sent) throw new IllegalStateException("Broker did not accept outbox event");
            event.setStatus("PUBLISHED"); event.setPublishedAt(Instant.now()); events.save(event);
        } catch (Exception exception) {
            int attempt = event.getAttemptCount() + 1; event.setAttemptCount(attempt);
            event.setStatus(attempt >= MAX_ATTEMPTS ? "FAILED" : "PENDING");
            event.setAvailableAt(Instant.now().plus(Math.min(300, 1L << Math.min(attempt, 8)), ChronoUnit.SECONDS)); events.save(event);
        }
    }
}
