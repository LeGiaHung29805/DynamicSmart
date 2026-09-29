package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import com.dynamicmart.order_service.service.OrderCreationAdmissionService.AdmissionResult;
import com.dynamicmart.order_service.service.OrderCreationPersistenceService.PersistedOrder;
import com.dynamicmart.order_service.service.OrderCreationRevalidationService.ValidatedOrderInput;
import com.dynamicmart.order_service.service.OrderPaymentCreationService.PaymentCommand;
import com.dynamicmart.order_service.service.OrderPaymentCreationService.PaymentCheckpoint;
import java.time.Instant;
import com.dynamicmart.order_service.service.OrderReservationService.ReservationResult;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Coordinates Create Order while keeping every remote call outside local database transactions. */
@Service
public class OrderCreationOrchestrator {
    private final OrderCreationAdmissionService admissions;
    private final OrderCreationRevalidationService revalidation;
    private final OrderReservationService reservations;
    private final ShippingQuoteConsumptionService quoteConsumption;
    private final OrderCreationPersistenceService persistence;
    private final OrderPaymentCreationService paymentCreation;
    private final OrderSagaRepository sagas;
    private final CustomerOrderRepository orders;

    public OrderCreationOrchestrator(
            OrderCreationAdmissionService admissions,
            OrderCreationRevalidationService revalidation,
            OrderReservationService reservations,
            ShippingQuoteConsumptionService quoteConsumption,
            OrderCreationPersistenceService persistence,
            OrderPaymentCreationService paymentCreation,
            OrderSagaRepository sagas,
            CustomerOrderRepository orders) {
        this.admissions = admissions;
        this.revalidation = revalidation;
        this.reservations = reservations;
        this.quoteConsumption = quoteConsumption;
        this.persistence = persistence;
        this.paymentCreation = paymentCreation;
        this.sagas = sagas;
        this.orders = orders;
    }

    public CreationResult create(UUID customerId, UUID checkoutSessionId, UUID idempotencyKey) {
        AdmissionResult admission = admissions.admit(customerId, checkoutSessionId, idempotencyKey);
        CommittedOrder committed = findCommittedOrder(customerId, admission);
        if (committed != null) {
            PaymentCheckpoint payment = paymentCreation.ensurePayment(paymentCommand(committed.order(), committed.saga()));
            return new CreationResult(
                    committed.order().getId(), committed.order().getOrderNumber(), committed.order().getStatus(),
                    committed.saga().getId(), payment.paymentId(), payment.paymentDueAt(), true);
        }

        ValidatedOrderInput input = revalidation.revalidate(customerId, admission.sagaId());
        ReservationResult reserved = reservations.reserve(input);
        try {
            quoteConsumption.consume(input);
        } catch (RuntimeException failure) {
            reservations.compensateAfterFailure(input, reserved, failure);
            throw failure;
        }

        PersistedOrder persisted = persistence.persist(input, reserved);
        PaymentCheckpoint payment = paymentCreation.ensurePayment(new PaymentCommand(
                admission.sagaId(), SagaStatus.ORDER_CREATED,
                "ORDER_SNAPSHOTS_PERSISTED", null, null, persisted.orderId(), input.context().customerId(),
                input.pricing().finalTotalVnd(), input.context().paymentTiming(), input.context().paymentMethod(),
                input.context().correlationId()));
        return new CreationResult(
                persisted.orderId(), persisted.orderNumber(), persisted.status(), admission.sagaId(),
                payment.paymentId(), payment.paymentDueAt(), admission.replay() || persisted.replay());
    }

    private CommittedOrder findCommittedOrder(UUID customerId, AdmissionResult admission) {
        if (!admission.replay()) {
            return null;
        }
        OrderSaga saga = sagas.findById(admission.sagaId())
                .orElseThrow(() -> conflict("ORDER_SAGA_NOT_FOUND", "Không tìm thấy Create Order Saga để replay."));
        if (saga.getOrderId() == null) {
            return null;
        }
        CustomerOrder order = orders.findByIdAndCustomerId(saga.getOrderId(), customerId)
                .orElseThrow(() -> conflict(
                        "ORDER_CREATION_STATE_MISMATCH", "Saga đã có orderId nhưng không tìm thấy Order tương ứng."));
        return new CommittedOrder(order, saga);
    }

    private PaymentCommand paymentCommand(CustomerOrder order, OrderSaga saga) {
        return new PaymentCommand(
                saga.getId(), saga.getStatus(), saga.getCurrentStep(), saga.getPaymentId(),
                order.getPaymentDueAt(), order.getId(),
                order.getCustomerId(), order.getFinalTotalVnd(), order.getPaymentTiming(), order.getPaymentMethod(),
                saga.getCorrelationId());
    }

    private OrderException conflict(String code, String message) {
        return new OrderException(HttpStatus.CONFLICT, code, message);
    }

    public record CreationResult(
            UUID orderId,
            String orderNumber,
            OrderStatus status,
            UUID sagaId,
            UUID paymentId,
            Instant paymentDueAt,
            boolean replay) {
    }

    private record CommittedOrder(CustomerOrder order, OrderSaga saga) {
    }
}
