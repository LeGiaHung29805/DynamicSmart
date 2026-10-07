package com.dynamicmart.engagement_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.engagement_service.dto.event.EventEnvelope;
import com.dynamicmart.engagement_service.dto.event.OrderCompletedPayload;
import com.dynamicmart.engagement_service.mapper.ReportMapper;
import com.dynamicmart.engagement_service.repository.DailyProductMetricRepository;
import com.dynamicmart.engagement_service.repository.DailySalesMetricRepository;
import com.dynamicmart.engagement_service.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportingServiceTest {
    private final DailySalesMetricRepository dailySales = org.mockito.Mockito.mock(DailySalesMetricRepository.class);
    private final DailyProductMetricRepository dailyProducts = org.mockito.Mockito.mock(DailyProductMetricRepository.class);
    private final ProcessedEventRepository processedEvents = org.mockito.Mockito.mock(ProcessedEventRepository.class);
    private final NotificationService notifications = org.mockito.Mockito.mock(NotificationService.class);
    private final ReportingService service = new ReportingService(dailySales, dailyProducts, processedEvents, notifications, new ReportMapper());

    @Test
    void duplicateOrderCompletedEventDoesNotUpdateReportsAgain() {
        UUID eventId = UUID.randomUUID();
        when(processedEvents.existsById(eventId)).thenReturn(true);
        var event = new EventEnvelope<>(eventId, "OrderCompleted", 1, "order-service", UUID.randomUUID(),
                Instant.now(), UUID.randomUUID(),
                new OrderCompletedPayload(UUID.randomUUID(), UUID.randomUUID(), 100_000L, 100_000L, 0L, 0L, List.of()));

        boolean processed = service.processOrderCompleted(event);

        assertThat(processed).isFalse();
        verify(dailySales, never()).save(org.mockito.Mockito.any());
        verify(dailyProducts, never()).save(org.mockito.Mockito.any());
    }

    @Test
    void processOrderCancelledCreatesNotificationForCustomer() {
        UUID eventId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(processedEvents.existsById(eventId)).thenReturn(false);

        var event = new EventEnvelope<>(eventId, "OrderCancelled", 1, "order-service", orderId,
                Instant.now(), UUID.randomUUID(),
                new com.dynamicmart.engagement_service.dto.event.OrderCancelledPayload(
                        orderId, "ORD-123", customerId, "Khách hàng đổi ý"));

        boolean processed = service.processOrderCancelled(event);

        assertThat(processed).isTrue();
        verify(notifications).create(org.mockito.ArgumentMatchers.eq(customerId),
                org.mockito.ArgumentMatchers.eq("ORDER_CANCELLED"),
                org.mockito.ArgumentMatchers.eq("Đơn hàng đã bị hủy"),
                org.mockito.ArgumentMatchers.contains("ORD-123"),
                org.mockito.ArgumentMatchers.any());
    }
}
