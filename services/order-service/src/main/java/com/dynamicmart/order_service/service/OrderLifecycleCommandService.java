package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.dto.response.OrderCommandResponse;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderActorType;
import com.dynamicmart.order_service.entity.OrderOperationLog;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.OrderStatusHistory;
import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderOperationLogRepository;
import com.dynamicmart.order_service.repository.OrderStatusHistoryRepository;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class OrderLifecycleCommandService {
    private static final String ADMIN_PACK = "ADMIN_PACK";
    private static final String ADMIN_SHIP = "ADMIN_SHIP";
    private static final String ADMIN_HANDOVER = "ADMIN_HANDOVER";
    private static final String CUSTOMER_CONFIRM_RECEIVED = "CUSTOMER_CONFIRM_RECEIVED";

    private final CustomerOrderRepository orders;
    private final OrderOperationLogRepository operationLogs;
    private final OrderStatusHistoryRepository histories;
    private final OutboxEventRepository outbox;
    private final OrderStateMachine stateMachine;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OrderLifecycleCommandService(
            CustomerOrderRepository orders,
            OrderOperationLogRepository operationLogs,
            OrderStatusHistoryRepository histories,
            OutboxEventRepository outbox,
            OrderStateMachine stateMachine,
            ObjectMapper objectMapper,
            Clock clock) {
        this.orders = orders;
        this.operationLogs = operationLogs;
        this.histories = histories;
        this.outbox = outbox;
        this.stateMachine = stateMachine;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public OrderCommandResponse adminPack(UUID adminId, UUID orderId, UUID operationKey) {
        return executeAdmin(adminId, orderId, operationKey, ADMIN_PACK,
                order -> List.of(stateMachine.adminPack(order.getStatus())));
    }

    @Transactional
    public OrderCommandResponse adminShip(UUID adminId, UUID orderId, UUID operationKey) {
        return executeAdmin(adminId, orderId, operationKey, ADMIN_SHIP,
                order -> List.of(stateMachine.adminShip(order.getStatus())));
    }

    @Transactional
    public OrderCommandResponse adminHandover(UUID adminId, UUID orderId, UUID operationKey) {
        return executeAdmin(adminId, orderId, operationKey, ADMIN_HANDOVER,
                order -> List.of(stateMachine.adminHandover(order.getStatus(), order.getPaymentTiming())));
    }

    @Transactional
    public OrderCommandResponse customerConfirmReceived(
            UUID customerId,
            UUID orderId,
            UUID operationKey) {
        requireIdentifiers(customerId, orderId, operationKey);
        CustomerOrder order = orders.findOwnedForUpdate(orderId, customerId)
                .orElseThrow(() -> notFound());
        OrderCommandResponse replay = replay(operationKey, CUSTOMER_CONFIRM_RECEIVED, orderId);
        if (replay != null) {
            return replay;
        }

        boolean paid = order.getPaymentSucceededAt() != null
                || order.getPaymentTiming() == PaymentTiming.NOT_REQUIRED;
        List<OrderStatus> transitions = stateMachine.customerConfirmReceived(
                order.getStatus(), order.getPaymentTiming(), paid);
        Instant now = Instant.now(clock);
        applyTransitions(order, transitions, OrderActorType.CUSTOMER, customerId,
                CUSTOMER_CONFIRM_RECEIVED, operationKey, null, now);
        if (transitions.contains(OrderStatus.DELIVERED)) {
            order.setShipmentDeliveredAt(now);
            emit(order, "ShipmentDelivered", operationKey, now);
        }
        if (transitions.contains(OrderStatus.COMPLETED)) {
            emit(order, "OrderCompleted", operationKey, now);
        }
        return record(operationKey, CUSTOMER_CONFIRM_RECEIVED, order, transitions, now);
    }

    private OrderCommandResponse executeAdmin(
            UUID adminId,
            UUID orderId,
            UUID operationKey,
            String operationType,
            Function<CustomerOrder, List<OrderStatus>> transition) {
        requireIdentifiers(adminId, orderId, operationKey);
        CustomerOrder order = orders.findForUpdate(orderId).orElseThrow(this::notFound);
        OrderCommandResponse replay = replay(operationKey, operationType, orderId);
        if (replay != null) {
            return replay;
        }

        List<OrderStatus> transitions = transition.apply(order);
        Instant now = Instant.now(clock);
        applyTransitions(order, transitions, OrderActorType.ADMIN, adminId,
                operationType, operationKey, null, now);
        if (ADMIN_HANDOVER.equals(operationType) && order.getPaymentMethod() == PaymentMethod.VNPAY) {
            emit(order, "PaymentDue", operationKey, now);
        }
        return record(operationKey, operationType, order, transitions, now);
    }

    private void applyTransitions(
            CustomerOrder order,
            List<OrderStatus> transitions,
            OrderActorType actorType,
            UUID actorId,
            String reason,
            UUID correlationId,
            UUID sourceEventId,
            Instant now) {
        for (OrderStatus target : transitions) {
            OrderStatus source = order.getStatus();
            order.setStatus(target);
            order.setUpdatedAt(now);
            if (target == OrderStatus.COMPLETED && order.getCompletedAt() == null) {
                order.setCompletedAt(now);
            }
            histories.save(OrderStatusHistory.transition(
                    UUID.randomUUID(), order.getId(), source, target, actorType, actorId,
                    reason, correlationId, sourceEventId, now));
        }
    }

    private void emit(CustomerOrder order, String eventType, UUID correlationId, Instant now) {
        var payload = new LifecycleCommandPayload(
                order.getId(), order.getOrderNumber(), order.getCustomerId(), order.getStatus(),
                order.getPaymentTiming(), order.getPaymentMethod(), order.getPaymentSucceededAt(),
                order.getShipmentDeliveredAt(), now);
        outbox.save(OutboxEvent.pending(
                UUID.randomUUID(), "ORDER", order.getId(), eventType, 1,
                objectMapper.writeValueAsString(payload), correlationId, now));
    }

    private OrderCommandResponse record(
            UUID operationKey,
            String operationType,
            CustomerOrder order,
            List<OrderStatus> transitions,
            Instant now) {
        OrderCommandResponse response = new OrderCommandResponse(
                order.getId(), order.getStatus(), List.copyOf(transitions), now, false);
        operationLogs.save(OrderOperationLog.completed(
                operationKey, operationType, order.getId(), objectMapper.writeValueAsString(response), now));
        return response;
    }

    private OrderCommandResponse replay(UUID operationKey, String operationType, UUID orderId) {
        OrderOperationLog existing = operationLogs.findById(operationKey).orElse(null);
        if (existing == null) {
            return null;
        }
        if (!Objects.equals(existing.getOperationType(), operationType)
                || !Objects.equals(existing.getOrderId(), orderId)) {
            throw conflict("IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key đã được dùng cho một thao tác Order khác.");
        }
        OrderCommandResponse stored = objectMapper.readValue(
                existing.getResultPayload(), OrderCommandResponse.class);
        return new OrderCommandResponse(
                stored.orderId(), stored.status(), stored.appliedTransitions(), stored.updatedAt(), true);
    }

    private void requireIdentifiers(UUID actorId, UUID orderId, UUID operationKey) {
        if (actorId == null || orderId == null || operationKey == null) {
            throw new OrderException(HttpStatus.BAD_REQUEST, "ORDER_COMMAND_INPUT_REQUIRED",
                    "Actor, Order ID và Idempotency-Key là bắt buộc.");
        }
    }

    private OrderException notFound() {
        return new OrderException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy Order.");
    }

    private OrderException conflict(String code, String message) {
        return new OrderException(HttpStatus.CONFLICT, code, message);
    }

    private record LifecycleCommandPayload(
            UUID orderId,
            String orderNumber,
            UUID customerId,
            OrderStatus status,
            PaymentTiming paymentTiming,
            PaymentMethod paymentMethod,
            Instant paymentSucceededAt,
            Instant shipmentDeliveredAt,
            Instant occurredAt) {
    }
}
