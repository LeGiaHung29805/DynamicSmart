package com.dynamicmart.notification_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.notification_service.dto.event.EventEnvelope;
import com.dynamicmart.notification_service.dto.event.OrderCancelledPayload;
import com.dynamicmart.notification_service.dto.event.OrderCompletedPayload;
import com.dynamicmart.notification_service.dto.event.PaymentSucceededPayload;
import com.dynamicmart.notification_service.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationEventServiceTest {
    private ProcessedEventRepository processedEvents;
    private NotificationService notifications;
    private NotificationEventService service;

    @BeforeEach
    void setUp() {
        processedEvents = org.mockito.Mockito.mock(ProcessedEventRepository.class);
        notifications = org.mockito.Mockito.mock(NotificationService.class);
        service = new NotificationEventService(processedEvents, notifications);
    }

    @Test
    void processOrderCompletedCreatesNotification() {
        UUID eventId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(processedEvents.insertIfAbsent(eq(eventId), eq("OrderCompleted"), eq("order-service"), any(), any()))
                .thenReturn(1);

        EventEnvelope<OrderCompletedPayload> event = new EventEnvelope<>(
                eventId, "OrderCompleted", 1, "order-service", orderId,
                Instant.now(), UUID.randomUUID(),
                new OrderCompletedPayload(orderId, "DM-DEMO-0001", customerId, 15_990_000L,
                        15_990_000L, 0L, 0L, List.of()));

        boolean result = service.processOrderCompleted(event);

        assertThat(result).isTrue();
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notifications).create(eq(customerId), eq("ORDER_COMPLETED"), eq("Đơn hàng đã hoàn tất"),
                contentCaptor.capture(), any());
        assertThat(contentCaptor.getValue()).contains("DM-DEMO-0001");
    }

    @Test
    void duplicateOrderCompletedReturnsFalse() {
        UUID eventId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(processedEvents.insertIfAbsent(eq(eventId), eq("OrderCompleted"), eq("order-service"), any(), any()))
                .thenReturn(0);

        EventEnvelope<OrderCompletedPayload> event = new EventEnvelope<>(
                eventId, "OrderCompleted", 1, "order-service", orderId,
                Instant.now(), UUID.randomUUID(),
                new OrderCompletedPayload(orderId, "DM-DEMO-0001", customerId, 15_990_000L,
                        15_990_000L, 0L, 0L, List.of()));

        boolean result = service.processOrderCompleted(event);

        assertThat(result).isFalse();
        verify(notifications, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void processPaymentSucceededCreatesNotificationWithFormattedAmount() {
        UUID eventId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(processedEvents.insertIfAbsent(eq(eventId), eq("PaymentSucceeded"), eq("payment-service"), any(), any()))
                .thenReturn(1);

        EventEnvelope<PaymentSucceededPayload> event = new EventEnvelope<>(
                eventId, "PaymentSucceeded", 1, "payment-service", orderId,
                Instant.now(), UUID.randomUUID(),
                new PaymentSucceededPayload(UUID.randomUUID(), orderId, customerId, 15_990_000L, "VNPAY"));

        boolean result = service.processPaymentSucceeded(event);

        assertThat(result).isTrue();
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notifications).create(eq(customerId), eq("PAYMENT_SUCCEEDED"), eq("Thanh toán thành công"),
                contentCaptor.capture(), any());
        assertThat(contentCaptor.getValue()).contains("15,990,000 đ");
    }

    @Test
    void processOrderCancelledCreatesNotificationWithReason() {
        UUID eventId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(processedEvents.insertIfAbsent(eq(eventId), eq("OrderCancelled"), eq("order-service"), any(), any()))
                .thenReturn(1);

        EventEnvelope<OrderCancelledPayload> event = new EventEnvelope<>(
                eventId, "OrderCancelled", 1, "order-service", orderId,
                Instant.now(), UUID.randomUUID(),
                new OrderCancelledPayload(orderId, "DM-DEMO-0003", customerId, "Hết hàng"));

        boolean result = service.processOrderCancelled(event);

        assertThat(result).isTrue();
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notifications).create(eq(customerId), eq("ORDER_CANCELLED"), eq("Đơn hàng đã bị hủy"),
                contentCaptor.capture(), any());
        assertThat(contentCaptor.getValue()).contains("DM-DEMO-0003").contains("Hết hàng");
    }

    @Test
    void invalidPayloadThrowsException() {
        EventEnvelope<OrderCompletedPayload> invalidEvent = new EventEnvelope<>(
                UUID.randomUUID(), "OrderCompleted", 1, "order-service", UUID.randomUUID(),
                Instant.now(), UUID.randomUUID(), null);

        assertThatThrownBy(() -> service.processOrderCompleted(invalidEvent))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
