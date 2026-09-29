package com.dynamicmart.order_service.application;

import com.dynamicmart.order_service.client.CartCheckoutClient;
import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderFinalizationPublisher {
    private static final int MAX_ATTEMPTS = 20;
    private final OutboxEventRepository events; private final ObjectMapper mapper; private final CartCheckoutClient cart;
    public OrderFinalizationPublisher(OutboxEventRepository events, ObjectMapper mapper, CartCheckoutClient cart) {
        this.events = events; this.mapper = mapper; this.cart = cart;
    }
    @Scheduled(fixedDelayString = "${app.outbox.finalization-delay-ms:1000}")
    public void publishPending() {
        events.findTop100ByStatusAndAvailableAtBeforeOrderByCreatedAtAsc("PENDING", Instant.now()).forEach(value -> publishOne(value.getId()));
    }
    public void publishOne(java.util.UUID eventId) {
        OutboxEvent event = events.findById(eventId).orElse(null);
        if (event == null || !"PENDING".equals(event.getStatus()) || event.getAvailableAt().isAfter(Instant.now())) return;
        try {
            OrderFinalizationQueue.FinalizationPayload payload = mapper.readValue(event.getPayload(), OrderFinalizationQueue.FinalizationPayload.class);
            payload.reservationIds().forEach(id -> cart.consume(id, payload.orderId()));
            cart.orderConfirmed(event.getId(), event.getCorrelationId(), payload.customerId(), payload.items());
            event.setStatus("PUBLISHED"); event.setPublishedAt(Instant.now()); events.save(event);
        } catch (Exception exception) {
            int attempt = event.getAttemptCount() + 1; event.setAttemptCount(attempt);
            event.setStatus(attempt >= MAX_ATTEMPTS ? "FAILED" : "PENDING");
            event.setAvailableAt(Instant.now().plus(Math.min(300, 1L << Math.min(attempt, 8)), ChronoUnit.SECONDS)); events.save(event);
        }
    }
}
