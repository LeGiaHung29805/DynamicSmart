package com.dynamicmart.payment_service.outbox;

import com.dynamicmart.payment_service.messaging.producer.PaymentEventSender;

import com.dynamicmart.payment_service.outbox.OutboxEvent;
import com.dynamicmart.payment_service.outbox.OutboxEventRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherTest {
    private OutboxEventRepository events;
    private PaymentEventSender eventSender;
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        events = mock(OutboxEventRepository.class);
        eventSender = mock(PaymentEventSender.class);
        publisher = new OutboxPublisher(events, eventSender, new ObjectMapper());
    }

    @Test
    void marksEventPublishedOnlyAfterBrokerAcceptsIt() {
        OutboxEvent event = event(0);
        when(events.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventSender.send(eq("PaymentSucceeded"), any())).thenReturn(true);

        publisher.publishOne(event.getId());

        assertEquals("PUBLISHED", event.getStatus());
        assertNotNull(event.getPublishedAt());
        assertEquals(0, event.getAttemptCount());
        verify(events).save(event);
    }

    @Test
    void reschedulesEventWhenBrokerRejectsIt() {
        OutboxEvent event = event(0);
        Instant previousAvailability = event.getAvailableAt();
        when(events.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventSender.send(eq("PaymentSucceeded"), any())).thenReturn(false);

        publisher.publishOne(event.getId());

        assertEquals("PENDING", event.getStatus());
        assertEquals(1, event.getAttemptCount());
        assertTrue(event.getAvailableAt().isAfter(previousAvailability));
        verify(events).save(event);
    }

    @Test
    void stopsRetryingAfterMaximumAttempts() {
        OutboxEvent event = event(9);
        when(events.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventSender.send(eq("PaymentSucceeded"), any())).thenReturn(false);

        publisher.publishOne(event.getId());

        assertEquals("FAILED", event.getStatus());
        assertEquals(10, event.getAttemptCount());
        verify(events).save(event);
    }

    private OutboxEvent event(int attemptCount) {
        Instant now = Instant.now();
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType("PAYMENT");
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("PaymentSucceeded");
        event.setEventVersion(1);
        event.setPayload("{\"paymentId\":\"" + UUID.randomUUID() + "\"}");
        event.setCorrelationId(UUID.randomUUID());
        event.setStatus("PENDING");
        event.setAttemptCount(attemptCount);
        event.setAvailableAt(now.minusSeconds(1));
        event.setCreatedAt(now.minusSeconds(2));
        return event;
    }
}
