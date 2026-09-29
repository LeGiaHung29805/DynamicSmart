package com.dynamicmart.order_service.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.dynamicmart.order_service.client.CartCheckoutClient;
import com.dynamicmart.order_service.entity.CheckoutSessionVoucher;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.repository.CheckoutSessionItemRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionVoucherRepository;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.ProcessedEventRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentEventConsumerTest {
    private final CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
    private final ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
    private final CheckoutSessionVoucherRepository vouchers = mock(CheckoutSessionVoucherRepository.class);
    private final CheckoutSessionItemRepository items = mock(CheckoutSessionItemRepository.class);
    private final CartCheckoutClient cart = mock(CartCheckoutClient.class);
    private final OrderFinalizationQueue finalization = mock(OrderFinalizationQueue.class);
    private final PaymentEventConsumer consumer = new PaymentEventConsumer(orders, processed, vouchers, items, cart, finalization);

    @Test
    void prepaidSuccessConfirmsOrderAndConsumesReservationOnce() {
        UUID eventId = UUID.randomUUID(), orderId = UUID.randomUUID(), sessionId = UUID.randomUUID(), reservationId = UUID.randomUUID();
        CustomerOrder order = pending(orderId, sessionId); CheckoutSessionVoucher voucher = new CheckoutSessionVoucher();
        voucher.setVoucherReservationId(reservationId);
        when(processed.existsById(eventId)).thenReturn(false, true);
        when(orders.findById(orderId)).thenReturn(Optional.of(order));
        when(vouchers.findAllByCheckoutSessionId(sessionId)).thenReturn(List.of(voucher));
        when(items.findAllByCheckoutSessionId(sessionId)).thenReturn(List.of());

        consumer.consume(event(eventId, "PaymentSucceeded", orderId));
        consumer.consume(event(eventId, "PaymentSucceeded", orderId));

        assertThat(order.getStatus()).isEqualTo("CONFIRMED");
        verify(finalization).enqueue(eq(eventId), eq(orderId), any(), eq(order.getCustomerId()), eq(List.of(reservationId)), eq(List.of()));
        verify(processed, times(2)).existsById(eventId);
        verify(processed).save(any());
    }

    @Test
    void prepaidFailureCancelsOrderAndReleasesReservation() {
        UUID eventId = UUID.randomUUID(), orderId = UUID.randomUUID(), sessionId = UUID.randomUUID(), reservationId = UUID.randomUUID();
        CustomerOrder order = pending(orderId, sessionId); CheckoutSessionVoucher voucher = new CheckoutSessionVoucher();
        voucher.setVoucherReservationId(reservationId);
        when(orders.findById(orderId)).thenReturn(Optional.of(order));
        when(vouchers.findAllByCheckoutSessionId(sessionId)).thenReturn(List.of(voucher));

        consumer.consume(event(eventId, "PaymentExpired", orderId));

        assertThat(order.getStatus()).isEqualTo("CANCELLED");
        assertThat(order.getCancelReason()).isEqualTo("PAYMENT_EXPIRED");
        verify(cart).release(reservationId, "PAYMENT_EXPIRED");
    }

    private CustomerOrder pending(UUID orderId, UUID sessionId) {
        CustomerOrder order = new CustomerOrder(); order.setId(orderId); order.setCheckoutSessionId(sessionId);
        order.setCustomerId(UUID.randomUUID()); order.setStatus("PENDING_PAYMENT"); return order;
    }
    private Map<String, Object> event(UUID eventId, String type, UUID orderId) {
        return Map.of("eventId", eventId.toString(), "eventType", type, "producer", "payment-service",
                "correlationId", UUID.randomUUID().toString(), "payload", Map.of("orderId", orderId.toString()));
    }
}
