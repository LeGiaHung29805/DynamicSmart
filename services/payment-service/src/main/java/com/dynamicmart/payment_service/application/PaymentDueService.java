package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.entity.ProcessedEvent;
import com.dynamicmart.payment_service.repository.ProcessedEventRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentDueService {
    private final ProcessedEventRepository processedEvents;
    private final PaymentService payments;

    public PaymentDueService(ProcessedEventRepository processedEvents, PaymentService payments) { this.processedEvents = processedEvents; this.payments = payments; }

    @Transactional
    public void consume(PaymentDueEvent event) {
        if (event == null || event.eventId() == null || event.orderId() == null || processedEvents.existsById(event.eventId())) return;
        payments.createVnPayAttemptForOrder(event.orderId());
        ProcessedEvent processed = new ProcessedEvent(); processed.setEventId(event.eventId()); processed.setEventType("PaymentDue"); processed.setProducer("order-service"); processed.setProcessedAt(Instant.now()); processed.setCorrelationId(event.correlationId()); processedEvents.save(processed);
    }
}
