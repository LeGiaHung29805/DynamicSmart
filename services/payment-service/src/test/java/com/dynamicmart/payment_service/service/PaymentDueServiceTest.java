package com.dynamicmart.payment_service.service;

import com.dynamicmart.payment_service.messaging.consumer.PaymentDueEvent;

import com.dynamicmart.payment_service.entity.ProcessedEvent;
import com.dynamicmart.payment_service.repository.ProcessedEventRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentDueServiceTest {
    private ProcessedEventRepository processedEvents;
    private PaymentService payments;
    private PaymentDueService service;

    @BeforeEach
    void setUp() {
        processedEvents = mock(ProcessedEventRepository.class);
        payments = mock(PaymentService.class);
        service = new PaymentDueService(processedEvents, payments);
    }

    @Test
    void createsPostpaidAttemptAndRecordsEvent() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        PaymentDueEvent event = new PaymentDueEvent(eventId, orderId, correlationId);

        service.consume(event);

        verify(payments).createVnPayAttemptForOrder(orderId);
        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEvents).save(captor.capture());
        assertEquals(eventId, captor.getValue().getEventId());
        assertEquals("PaymentDue", captor.getValue().getEventType());
        assertEquals("order-service", captor.getValue().getProducer());
        assertEquals(correlationId, captor.getValue().getCorrelationId());
        assertNotNull(captor.getValue().getProcessedAt());
    }

    @Test
    void ignoresEventAlreadyProcessed() {
        UUID eventId = UUID.randomUUID();
        when(processedEvents.existsById(eventId)).thenReturn(true);

        service.consume(new PaymentDueEvent(eventId, UUID.randomUUID(), UUID.randomUUID()));

        verify(payments, never()).createVnPayAttemptForOrder(org.mockito.ArgumentMatchers.any());
        verify(processedEvents, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ignoresMalformedEvent() {
        service.consume(new PaymentDueEvent(null, UUID.randomUUID(), UUID.randomUUID()));

        verify(payments, never()).createVnPayAttemptForOrder(org.mockito.ArgumentMatchers.any());
        verify(processedEvents, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
