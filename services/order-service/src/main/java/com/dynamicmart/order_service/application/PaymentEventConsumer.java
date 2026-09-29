package com.dynamicmart.order_service.application;

import com.dynamicmart.order_service.client.CartCheckoutClient;
import com.dynamicmart.order_service.entity.CheckoutSessionVoucher;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.ProcessedEvent;
import com.dynamicmart.order_service.repository.CheckoutSessionItemRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionVoucherRepository;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class PaymentEventConsumer {
    private final CustomerOrderRepository orders; private final ProcessedEventRepository processed;
    private final CheckoutSessionVoucherRepository vouchers; private final CheckoutSessionItemRepository items;
    private final CartCheckoutClient cart; private final OrderFinalizationQueue finalization;
    public PaymentEventConsumer(CustomerOrderRepository orders, ProcessedEventRepository processed,
                                CheckoutSessionVoucherRepository vouchers, CheckoutSessionItemRepository items,
                                CartCheckoutClient cart, OrderFinalizationQueue finalization) {
        this.orders = orders; this.processed = processed; this.vouchers = vouchers; this.items = items; this.cart = cart; this.finalization = finalization;
    }

    @Bean Consumer<Map<String, Object>> paymentEvents() { return this::consume; }

    @Transactional
    public void consume(Map<String, Object> envelope) {
        String eventType = String.valueOf(envelope.get("eventType"));
        if (!List.of("PaymentSucceeded", "PaymentFailed", "PaymentExpired").contains(eventType)) return;
        UUID eventId = uuid(envelope.get("eventId"));
        if (eventId == null || processed.existsById(eventId)) return;
        Object rawPayload = envelope.get("payload");
        if (!(rawPayload instanceof Map<?, ?> payload)) return;
        UUID orderId = uuid(payload.get("orderId"));
        CustomerOrder order = orderId == null ? null : orders.findById(orderId).orElse(null);
        if (order == null) { mark(eventId, eventType, uuid(envelope.get("correlationId"))); return; }
        Instant now = Instant.now();
        List<CheckoutSessionVoucher> reservations = vouchers.findAllByCheckoutSessionId(order.getCheckoutSessionId());
        if ("PaymentSucceeded".equals(eventType) && "PENDING_PAYMENT".equals(order.getStatus())) {
            order.setStatus("CONFIRMED"); order.setConfirmedAt(now); order.setPaymentSucceededAt(now); order.setUpdatedAt(now); orders.save(order);
            var purchased = items.findAllByCheckoutSessionId(order.getCheckoutSessionId()).stream()
                    .filter(value -> value.getSourceCartItemId() != null)
                    .map(value -> new CartCheckoutClient.PurchasedItem(value.getSourceCartItemId(), value.getQuantity(), value.getSourceCartItemVersion())).toList();
            finalization.enqueue(eventId, order.getId(), uuid(envelope.get("correlationId")), order.getCustomerId(),
                    reservations.stream().map(CheckoutSessionVoucher::getVoucherReservationId).filter(java.util.Objects::nonNull).toList(), purchased);
        } else if (("PaymentFailed".equals(eventType) || "PaymentExpired".equals(eventType)) && "PENDING_PAYMENT".equals(order.getStatus())) {
            order.setStatus("CANCELLED"); order.setCancelReason(eventType.equals("PaymentExpired") ? "PAYMENT_EXPIRED" : "PAYMENT_FAILED");
            order.setCancelledAt(now); order.setUpdatedAt(now); orders.save(order);
            reservations.stream().map(CheckoutSessionVoucher::getVoucherReservationId).filter(java.util.Objects::nonNull)
                    .forEach(id -> cart.release(id, order.getCancelReason()));
        }
        mark(eventId, eventType, uuid(envelope.get("correlationId")));
    }

    private void mark(UUID id, String type, UUID correlationId) {
        ProcessedEvent value = new ProcessedEvent(); value.setEventId(id); value.setEventType(type);
        value.setProducer("payment-service"); value.setProcessedAt(Instant.now()); value.setCorrelationId(correlationId); processed.save(value);
    }
    private UUID uuid(Object value) { try { return value == null ? null : UUID.fromString(String.valueOf(value)); } catch (IllegalArgumentException ignored) { return null; } }
}
