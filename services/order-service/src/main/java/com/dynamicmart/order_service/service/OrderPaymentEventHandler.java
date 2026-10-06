package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.messaging.PaymentEventEnvelope;
import com.dynamicmart.order_service.service.OrderPaymentEventService.FollowUpAction;
import com.dynamicmart.order_service.service.OrderPaymentEventService.PaymentEventResult;
import org.springframework.stereotype.Service;

/** Keeps remote reservation I/O outside the local Payment-event transaction. */
@Service
public class OrderPaymentEventHandler {
    private final OrderPaymentEventService events;
    private final OrderReservationFinalizationService finalization;
    private final OrderCompensationRecoveryService compensation;

    public OrderPaymentEventHandler(
            OrderPaymentEventService events,
            OrderReservationFinalizationService finalization,
            OrderCompensationRecoveryService compensation) {
        this.events = events;
        this.finalization = finalization;
        this.compensation = compensation;
    }

    public void handle(PaymentEventEnvelope event) {
        if (!events.supports(event)) {
            return;
        }
        PaymentEventResult result = events.apply(event);
        if (result.followUpAction() == FollowUpAction.FINALIZE_RESERVATIONS) {
            finalization.finalizeIfConfirmed(result.sagaId(), result.orderId());
        } else if (result.followUpAction() == FollowUpAction.COMPENSATE_RESERVATIONS) {
            compensation.resume(result.sagaId());
        }
    }
}
