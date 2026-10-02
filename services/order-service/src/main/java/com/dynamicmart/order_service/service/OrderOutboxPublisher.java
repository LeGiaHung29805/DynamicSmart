package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.CartOrderConfirmationGateway;
import com.dynamicmart.order_service.client.CartOrderConfirmationGateway.OrderConfirmedCommand;
import com.dynamicmart.order_service.config.OutboxProperties;
import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.entity.OutboxEventStatus;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import com.dynamicmart.order_service.service.OrderConfirmationEventService.OrderConfirmedPayload;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderOutboxPublisher {
    private static final int MAX_ATTEMPTS = 10;
    private static final String ORDER_CONFIRMED = "OrderConfirmed";
    private static final String PAYMENT_DUE = "PaymentDue";

    private final OutboxEventRepository events;
    private final CartOrderConfirmationGateway cart;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;
    private final OutboxProperties properties;
    private final Clock clock;

    public OrderOutboxPublisher(
            OutboxEventRepository events,
            CartOrderConfirmationGateway cart,
            StreamBridge streamBridge,
            ObjectMapper objectMapper,
            OutboxProperties properties,
            Clock clock) {
        this.events = events;
        this.cart = cart;
        this.streamBridge = streamBridge;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay:1s}")
    public void publishPending() {
        Instant now = Instant.now(clock);
        events.findAllByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                        OutboxEventStatus.PENDING, now, PageRequest.of(0, properties.batchSize()))
                .forEach(event -> publishOne(event.getId()));
    }

    public void publishOne(UUID eventId) {
        OutboxEvent event = events.findById(eventId).orElse(null);
        Instant now = Instant.now(clock);
        if (event == null
                || event.getStatus() != OutboxEventStatus.PENDING
                || event.getAvailableAt().isAfter(now)) {
            return;
        }

        try {
            dispatch(event);
            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(now);
            events.save(event);
        } catch (RuntimeException failure) {
            int attempt = Math.addExact(event.getAttemptCount(), 1);
            event.setAttemptCount(attempt);
            event.setStatus(attempt >= MAX_ATTEMPTS ? OutboxEventStatus.FAILED : OutboxEventStatus.PENDING);
            event.setAvailableAt(now.plusSeconds(Math.min(300, 1L << Math.min(attempt, 8))));
            events.save(event);
        }
    }

    private void dispatch(OutboxEvent event) {
        if (ORDER_CONFIRMED.equals(event.getEventType())) {
            dispatchOrderConfirmed(event);
            return;
        }
        if (PAYMENT_DUE.equals(event.getEventType())) {
            boolean sent = streamBridge.send(
                    "paymentDue-out-0",
                    MessageBuilder.withPayload(new PaymentDueMessage(
                            event.getId(), event.getAggregateId(), event.getCorrelationId())).build());
            requireSent(sent);
            return;
        }

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", event.getId());
        envelope.put("eventType", event.getEventType());
        envelope.put("eventVersion", event.getEventVersion());
        envelope.put("producer", "order-service");
        envelope.put("aggregateId", event.getAggregateId());
        envelope.put("occurredAt", event.getCreatedAt());
        envelope.put("correlationId", event.getCorrelationId());
        envelope.put("payload", objectMapper.readTree(event.getPayload()));
        boolean sent = streamBridge.send(
                "orderEvents-out-0",
                MessageBuilder.withPayload(envelope)
                        .setHeader("eventType", event.getEventType())
                        .build());
        requireSent(sent);
    }

    private void dispatchOrderConfirmed(OutboxEvent event) {
        OrderConfirmedPayload payload = objectMapper.readValue(
                event.getPayload(), OrderConfirmedPayload.class);
        cart.confirm(new OrderConfirmedCommand(
                event.getId(), event.getCorrelationId(), payload.customerId(), payload.source(), payload.items()));
    }

    private void requireSent(boolean sent) {
        if (!sent) {
            throw new IllegalStateException("Broker không nhận Outbox event.");
        }
    }

    private record PaymentDueMessage(UUID eventId, UUID orderId, UUID correlationId) {
    }
}
