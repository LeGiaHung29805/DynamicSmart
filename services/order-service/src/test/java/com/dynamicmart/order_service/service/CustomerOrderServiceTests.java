package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.client.PaymentClient;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.OrderStatusHistory;
import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderStatusHistoryRepository;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import tools.jackson.databind.ObjectMapper;

class CustomerOrderServiceTests {
    @Test
    void confirmingReceivedCompletesCodWithoutAmountOrReceiptInput() {
        CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
        OrderStatusHistoryRepository histories = mock(OrderStatusHistoryRepository.class);
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        PaymentClient payments = mock(PaymentClient.class);
        Instant now = Instant.parse("2026-10-03T00:00:00Z");
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CustomerOrder order = CustomerOrder.create(orderId, "ORD-TEST", UUID.randomUUID(), customerId,
                OrderStatus.HANDOVER_PENDING, PaymentTiming.POSTPAID, PaymentMethod.COD,
                100_000, 0, 100_000, 0, 0, 20_000, 0, 120_000, null, now.minusSeconds(60));
        when(orders.findForUpdate(orderId)).thenReturn(Optional.of(order));

        CustomerOrderService service = new CustomerOrderService(orders, histories, outbox, payments,
                new OrderStateMachine(), new ObjectMapper(), Clock.fixed(now, ZoneOffset.UTC));
        var response = service.confirmReceived(orderId, customerId);

        assertEquals(OrderStatus.COMPLETED, response.status());
        assertEquals(now, response.shipmentDeliveredAt());
        assertEquals(now, response.completedAt());
        verify(payments).collectCodForReceivedOrder(orderId);
        verify(histories, times(2)).save(any(OrderStatusHistory.class));
        verify(outbox).save(any(OutboxEvent.class));
    }

    @Test
    void invalidOrderStateDoesNotMarkCodPaid() {
        CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
        PaymentClient payments = mock(PaymentClient.class);
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-03T00:00:00Z");
        CustomerOrder order = CustomerOrder.create(orderId, "ORD-TEST", UUID.randomUUID(), customerId,
                OrderStatus.SHIPPING, PaymentTiming.POSTPAID, PaymentMethod.COD,
                100_000, 0, 100_000, 0, 0, 20_000, 0, 120_000, null, now.minusSeconds(60));
        when(orders.findForUpdate(orderId)).thenReturn(Optional.of(order));
        CustomerOrderService service = new CustomerOrderService(orders, mock(OrderStatusHistoryRepository.class),
                mock(OutboxEventRepository.class), payments, new OrderStateMachine(), new ObjectMapper(),
                Clock.fixed(now, ZoneOffset.UTC));

        Assertions.assertThrows(RuntimeException.class, () -> service.confirmReceived(orderId, customerId));

        verify(payments, never()).collectCodForReceivedOrder(orderId);
    }
}
