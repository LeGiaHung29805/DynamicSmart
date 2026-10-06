package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.CartOrderConfirmationGateway;
import com.dynamicmart.order_service.client.CartOrderConfirmationGateway.OrderConfirmedCommand;
import com.dynamicmart.order_service.config.OutboxProperties;
import com.dynamicmart.order_service.service.OrderOutboxClaimService.OutboxClaim;
import com.dynamicmart.order_service.service.OrderConfirmationEventService.OrderConfirmedPayload;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderOutboxPublisher {
    private static final String ORDER_CONFIRMED = "OrderConfirmed";
    private static final String PAYMENT_DUE = "PaymentDue";

    private final OrderOutboxClaimService claims;
    private final CartOrderConfirmationGateway cart;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;
    private final OutboxProperties properties;

    public OrderOutboxPublisher(
            OrderOutboxClaimService claims,
            CartOrderConfirmationGateway cart,
            StreamBridge streamBridge,
            ObjectMapper objectMapper,
            OutboxProperties properties) {
        this.claims = claims;
        this.cart = cart;
        this.streamBridge = streamBridge;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay:1s}")
    public void publishPending() {
        for (int index = 0; index < properties.batchSize(); index++) {
            OutboxClaim claim = claims.claimNext().orElse(null);
            if (claim == null) {
                return;
            }
            publishClaim(claim);
        }
    }

    public void publishOne(UUID eventId) {
        claims.claim(eventId).ifPresent(this::publishClaim);
    }

    private void publishClaim(OutboxClaim claim) {
        try {
            dispatch(claim);
            claims.markPublished(claim.eventId(), claim.owner());
        } catch (RuntimeException failure) {
            claims.markFailed(claim.eventId(), claim.owner());
        }
    }

    private void dispatch(OutboxClaim claim) {
        if (ORDER_CONFIRMED.equals(claim.eventType())) {
            dispatchOrderConfirmed(claim);
            return;
        }
        if (PAYMENT_DUE.equals(claim.eventType())) {
            boolean sent = streamBridge.send(
                    "paymentDue-out-0",
                    MessageBuilder.withPayload(new PaymentDueMessage(
                            claim.eventId(), claim.aggregateId(), claim.correlationId())).build());
            requireSent(sent);
            return;
        }

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", claim.eventId());
        envelope.put("eventType", claim.eventType());
        envelope.put("eventVersion", claim.eventVersion());
        envelope.put("producer", "order-service");
        envelope.put("aggregateId", claim.aggregateId());
        envelope.put("occurredAt", claim.createdAt());
        envelope.put("correlationId", claim.correlationId());
        envelope.put("payload", objectMapper.readTree(claim.payload()));
        boolean sent = streamBridge.send(
                "orderEvents-out-0",
                MessageBuilder.withPayload(envelope)
                        .setHeader("eventType", claim.eventType())
                        .build());
        requireSent(sent);
    }

    private void dispatchOrderConfirmed(OutboxClaim claim) {
        OrderConfirmedPayload payload = objectMapper.readValue(
                claim.payload(), OrderConfirmedPayload.class);
        cart.confirm(new OrderConfirmedCommand(
                claim.eventId(), claim.correlationId(), payload.customerId(), payload.source(), payload.items()));
    }

    private void requireSent(boolean sent) {
        if (!sent) {
            throw new IllegalStateException("Broker không nhận Outbox event.");
        }
    }

    private record PaymentDueMessage(UUID eventId, UUID orderId, UUID correlationId) {
    }
}
