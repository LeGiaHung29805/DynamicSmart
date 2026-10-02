package com.dynamicmart.order_service.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.messaging.PaymentEventEnvelope;
import com.dynamicmart.order_service.service.OrderPaymentEventService.FollowUpAction;
import com.dynamicmart.order_service.service.OrderPaymentEventService.PaymentEventResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OrderPaymentEventHandlerTests {
    @Test
    void confirmedPrepaidEventFinalizesOutsideCheckpointService() {
        Fixture fixture = fixture(FollowUpAction.FINALIZE_RESERVATIONS);

        fixture.handler.handle(fixture.event);

        verify(fixture.finalization).finalizeIfConfirmed(fixture.sagaId, fixture.orderId);
        verify(fixture.compensation, never()).resume(fixture.sagaId);
    }

    @Test
    void cancelledPrepaidEventResumesCompensation() {
        Fixture fixture = fixture(FollowUpAction.COMPENSATE_RESERVATIONS);

        fixture.handler.handle(fixture.event);

        verify(fixture.compensation).resume(fixture.sagaId);
        verify(fixture.finalization, never()).finalizeIfConfirmed(fixture.sagaId, fixture.orderId);
    }

    @Test
    void unrelatedEventOnSharedDestinationIsIgnored() {
        Fixture fixture = fixture(FollowUpAction.NONE);
        when(fixture.events.supports(fixture.event)).thenReturn(false);

        fixture.handler.handle(fixture.event);

        verify(fixture.events, never()).apply(fixture.event);
        verify(fixture.finalization, never()).finalizeIfConfirmed(fixture.sagaId, fixture.orderId);
        verify(fixture.compensation, never()).resume(fixture.sagaId);
    }

    private Fixture fixture(FollowUpAction action) {
        OrderPaymentEventService events = Mockito.mock(OrderPaymentEventService.class);
        OrderReservationFinalizationService finalization = Mockito.mock(OrderReservationFinalizationService.class);
        OrderCompensationRecoveryService compensation = Mockito.mock(OrderCompensationRecoveryService.class);
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        PaymentEventEnvelope event = Mockito.mock(PaymentEventEnvelope.class);
        when(events.supports(event)).thenReturn(true);
        when(events.apply(event)).thenReturn(new PaymentEventResult(
                orderId, sagaId, OrderStatus.CONFIRMED, action, false));
        return new Fixture(
                new OrderPaymentEventHandler(events, finalization, compensation), events,
                finalization, compensation, event, orderId, sagaId);
    }

    private record Fixture(
            OrderPaymentEventHandler handler,
            OrderPaymentEventService events,
            OrderReservationFinalizationService finalization,
            OrderCompensationRecoveryService compensation,
            PaymentEventEnvelope event,
            UUID orderId,
            UUID sagaId) {
    }
}
