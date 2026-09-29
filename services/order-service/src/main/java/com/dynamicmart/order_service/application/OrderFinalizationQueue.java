package com.dynamicmart.order_service.application;

import com.dynamicmart.order_service.client.CartCheckoutClient;
import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class OrderFinalizationQueue {
    private final OutboxEventRepository events; private final ObjectMapper mapper;
    public OrderFinalizationQueue(OutboxEventRepository events, ObjectMapper mapper) { this.events = events; this.mapper = mapper; }
    public void enqueue(UUID eventId, UUID orderId, UUID correlationId, UUID customerId, List<UUID> reservations,
                        List<CartCheckoutClient.PurchasedItem> items) {
        Instant now = Instant.now(); OutboxEvent event = new OutboxEvent(); event.setId(eventId); event.setAggregateType("ORDER");
        event.setAggregateId(orderId); event.setEventType("OrderConfirmed"); event.setEventVersion(1);
        try { event.setPayload(mapper.writeValueAsString(new FinalizationPayload(orderId, customerId, reservations, items))); }
        catch (JacksonException exception) { throw new IllegalStateException(exception); }
        event.setCorrelationId(correlationId); event.setStatus("PENDING"); event.setAttemptCount(0);
        event.setAvailableAt(now); event.setCreatedAt(now); events.save(event);
    }
    public record FinalizationPayload(UUID orderId, UUID customerId, List<UUID> reservationIds,
                                      List<CartCheckoutClient.PurchasedItem> items) { }
}
