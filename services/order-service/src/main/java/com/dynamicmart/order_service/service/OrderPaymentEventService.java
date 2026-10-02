package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderActorType;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.OrderStatusHistory;
import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.ProcessedEvent;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.messaging.PaymentEventEnvelope;
import com.dynamicmart.order_service.messaging.PaymentEventEnvelope.PaymentPayload;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import com.dynamicmart.order_service.repository.OrderStatusHistoryRepository;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import com.dynamicmart.order_service.repository.ProcessedEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Applies Payment events exactly once to the Order aggregate in a short local transaction. */
@Service
public class OrderPaymentEventService {
    static final String PAYMENT_SUCCEEDED = "PaymentSucceeded";
    static final String PAYMENT_FAILED = "PaymentFailed";
    static final String PAYMENT_EXPIRED = "PaymentExpired";
    private static final String PAYMENT_PRODUCER = "payment-service";
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            PAYMENT_SUCCEEDED, PAYMENT_FAILED, PAYMENT_EXPIRED);

    private final CustomerOrderRepository orders;
    private final OrderSagaRepository sagas;
    private final OrderStatusHistoryRepository histories;
    private final OutboxEventRepository outbox;
    private final ProcessedEventRepository processedEvents;
    private final OrderStateMachine stateMachine;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OrderPaymentEventService(
            CustomerOrderRepository orders,
            OrderSagaRepository sagas,
            OrderStatusHistoryRepository histories,
            OutboxEventRepository outbox,
            ProcessedEventRepository processedEvents,
            OrderStateMachine stateMachine,
            ObjectMapper objectMapper,
            Clock clock) {
        this.orders = orders;
        this.sagas = sagas;
        this.histories = histories;
        this.outbox = outbox;
        this.processedEvents = processedEvents;
        this.stateMachine = stateMachine;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public boolean supports(PaymentEventEnvelope event) {
        return event != null
                && PAYMENT_PRODUCER.equals(event.producer())
                && SUPPORTED_EVENTS.contains(event.eventType());
    }

    @Transactional
    public PaymentEventResult apply(PaymentEventEnvelope event) {
        requireEnvelope(event);
        PaymentPayload payload = event.payload();
        CustomerOrder order = orders.findForUpdate(payload.orderId())
                .orElseThrow(() -> notFound("ORDER_NOT_FOUND", "Không tìm thấy Order của Payment event."));
        OrderSaga sagaReference = sagas.findByOrderId(order.getId())
                .orElseThrow(() -> conflict("ORDER_SAGA_NOT_FOUND", "Order chưa có Create Order Saga."));
        OrderSaga saga = sagas.findByIdForUpdate(sagaReference.getId())
                .orElseThrow(() -> conflict("ORDER_SAGA_NOT_FOUND", "Không thể khóa Create Order Saga."));
        requireMatchingContract(event, order, saga);

        ProcessedEvent existing = processedEvents.findById(event.eventId()).orElse(null);
        if (existing != null) {
            requireSameProcessedEvent(existing, event);
            return resultForCurrentState(event.eventType(), order, saga, true);
        }

        Instant now = Instant.now(clock);
        if (PAYMENT_SUCCEEDED.equals(event.eventType())) {
            applySucceeded(event, order, now);
        } else {
            applyFailedOrExpired(event, order, saga, now);
        }
        processedEvents.save(ProcessedEvent.record(
                event.eventId(), event.eventType(), event.producer(), now, event.correlationId()));
        return resultForCurrentState(event.eventType(), order, saga, false);
    }

    private void applySucceeded(PaymentEventEnvelope event, CustomerOrder order, Instant now) {
        requirePayloadStatus(event, "PAID");
        Instant occurredAt = event.occurredAt();
        if (order.getPaymentSucceededAt() == null || occurredAt.isBefore(order.getPaymentSucceededAt())) {
            order.setPaymentSucceededAt(occurredAt);
        }
        List<OrderStatus> transitions = stateMachine.onPaymentSucceeded(
                order.getStatus(), order.getPaymentTiming(), order.getPaymentMethod());
        for (OrderStatus target : transitions) {
            transition(order, target, event, now);
        }
    }

    private void applyFailedOrExpired(
            PaymentEventEnvelope event,
            CustomerOrder order,
            OrderSaga saga,
            Instant now) {
        requirePayloadStatus(event, PAYMENT_FAILED.equals(event.eventType()) ? "FAILED" : "EXPIRED");
        var target = stateMachine.onPaymentFailedOrExpired(order.getStatus(), order.getPaymentTiming());
        if (target.isEmpty()) {
            return;
        }
        String reason = PAYMENT_FAILED.equals(event.eventType()) ? "PAYMENT_FAILED" : "PAYMENT_EXPIRED";
        order.setCancelReason(reason);
        order.setCancelledAt(event.occurredAt());
        transition(order, target.orElseThrow(), event, now);
        saga.beginPaymentCompensation(reason, now);
    }

    private void transition(
            CustomerOrder order,
            OrderStatus target,
            PaymentEventEnvelope event,
            Instant now) {
        OrderStatus source = order.getStatus();
        order.setStatus(target);
        order.setUpdatedAt(now);
        if (target == OrderStatus.CONFIRMED && order.getConfirmedAt() == null) {
            order.setConfirmedAt(event.occurredAt());
        } else if (target == OrderStatus.COMPLETED && order.getCompletedAt() == null) {
            order.setCompletedAt(event.occurredAt());
        }
        histories.save(OrderStatusHistory.transition(
                UUID.randomUUID(), order.getId(), source, target, OrderActorType.PAYMENT_EVENT,
                event.payload().paymentId(), event.eventType(), event.correlationId(), event.eventId(), now));
        if (target == OrderStatus.CANCELLED || target == OrderStatus.COMPLETED) {
            emitLifecycleEvent(order, target == OrderStatus.CANCELLED ? "OrderCancelled" : "OrderCompleted",
                    event.correlationId(), now);
        }
    }

    private void emitLifecycleEvent(
            CustomerOrder order,
            String eventType,
            UUID correlationId,
            Instant now) {
        var payload = new OrderLifecyclePayload(
                order.getId(), order.getOrderNumber(), order.getCustomerId(), order.getStatus(),
                order.getCheckoutSessionId(), order.getFinalTotalVnd(), order.getCurrency(),
                order.getCancelReason(), now);
        outbox.save(OutboxEvent.pending(
                UUID.randomUUID(), "ORDER", order.getId(), eventType, 1,
                objectMapper.writeValueAsString(payload), correlationId, now));
    }

    private PaymentEventResult resultForCurrentState(
            String eventType,
            CustomerOrder order,
            OrderSaga saga,
            boolean replay) {
        FollowUpAction action = FollowUpAction.NONE;
        if (PAYMENT_SUCCEEDED.equals(eventType)
                && order.getPaymentTiming() == PaymentTiming.PREPAID
                && order.getStatus() == OrderStatus.CONFIRMED
                && saga.getStatus() != SagaStatus.COMPLETED) {
            action = FollowUpAction.FINALIZE_RESERVATIONS;
        } else if (!PAYMENT_SUCCEEDED.equals(eventType)
                && order.getStatus() == OrderStatus.CANCELLED
                && saga.getStatus() == SagaStatus.COMPENSATING) {
            action = FollowUpAction.COMPENSATE_RESERVATIONS;
        }
        return new PaymentEventResult(order.getId(), saga.getId(), order.getStatus(), action, replay);
    }

    private void requireEnvelope(PaymentEventEnvelope event) {
        if (!supports(event)
                || event.eventId() == null
                || event.eventVersion() != 1
                || event.aggregateId() == null
                || event.occurredAt() == null
                || event.correlationId() == null
                || event.payload() == null
                || event.payload().paymentId() == null
                || event.payload().orderId() == null
                || event.payload().timing() == null
                || event.payload().method() == null
                || event.payload().status() == null
                || event.payload().amountVnd() < 0) {
            throw new OrderException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PAYMENT_EVENT",
                    "Payment event version 1 không đầy đủ hoặc không hợp lệ.");
        }
    }

    private void requireMatchingContract(
            PaymentEventEnvelope event,
            CustomerOrder order,
            OrderSaga saga) {
        PaymentPayload payload = event.payload();
        PaymentTiming timing = parseTiming(payload.timing());
        PaymentMethod method = parseMethod(payload.method());
        if (!Objects.equals(event.aggregateId(), payload.paymentId())
                || !Objects.equals(saga.getOrderId(), order.getId())
                || !Objects.equals(saga.getPaymentId(), payload.paymentId())
                || !Objects.equals(saga.getCorrelationId(), event.correlationId())
                || payload.amountVnd() != order.getFinalTotalVnd()
                || timing != order.getPaymentTiming()
                || method != order.getPaymentMethod()) {
            throw conflict(
                    "PAYMENT_EVENT_CONTRACT_MISMATCH",
                    "Payment event không khớp payment/order snapshot đã lưu.");
        }
    }

    private void requireSameProcessedEvent(ProcessedEvent existing, PaymentEventEnvelope event) {
        if (!Objects.equals(existing.getEventType(), event.eventType())
                || !Objects.equals(existing.getProducer(), event.producer())
                || !Objects.equals(existing.getCorrelationId(), event.correlationId())) {
            throw conflict("PAYMENT_EVENT_ID_REUSED", "eventId đã được dùng cho một event khác.");
        }
    }

    private void requirePayloadStatus(PaymentEventEnvelope event, String expected) {
        if (!expected.equals(event.payload().status())) {
            throw conflict(
                    "PAYMENT_EVENT_STATUS_MISMATCH",
                    event.eventType() + " phải mang trạng thái Payment " + expected + ".");
        }
    }

    private PaymentTiming parseTiming(String value) {
        try {
            return PaymentTiming.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw conflict("PAYMENT_EVENT_CONTRACT_MISMATCH", "Payment timing không được Order hỗ trợ.");
        }
    }

    private PaymentMethod parseMethod(String value) {
        try {
            return PaymentMethod.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw conflict("PAYMENT_EVENT_CONTRACT_MISMATCH", "Payment method không được Order hỗ trợ.");
        }
    }

    private OrderException notFound(String code, String message) {
        return new OrderException(HttpStatus.NOT_FOUND, code, message);
    }

    private OrderException conflict(String code, String message) {
        return new OrderException(HttpStatus.CONFLICT, code, message);
    }

    public enum FollowUpAction {
        NONE,
        FINALIZE_RESERVATIONS,
        COMPENSATE_RESERVATIONS
    }

    public record PaymentEventResult(
            UUID orderId,
            UUID sagaId,
            OrderStatus orderStatus,
            FollowUpAction followUpAction,
            boolean replay) {
    }

    private record OrderLifecyclePayload(
            UUID orderId,
            String orderNumber,
            UUID customerId,
            OrderStatus status,
            UUID checkoutSessionId,
            long finalTotalVnd,
            String currency,
            String reason,
            Instant occurredAt) {
    }
}
