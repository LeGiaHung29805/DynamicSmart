package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores durable progress around remote Inventory commit and Voucher consume calls. */
@Service
public class OrderReservationFinalizationCheckpointService {
    private final OrderSagaRepository sagas;
    private final Clock clock;

    public OrderReservationFinalizationCheckpointService(OrderSagaRepository sagas, Clock clock) {
        this.sagas = sagas;
        this.clock = clock;
    }

    @Transactional
    public void begin(UUID sagaId, UUID orderId) {
        OrderSaga saga = lock(sagaId);
        requireOrder(saga, orderId);
        if (saga.getStatus() == SagaStatus.FINALIZING_RESERVATIONS
                || saga.getStatus() == SagaStatus.INVENTORY_COMMITTED
                || saga.getStatus() == SagaStatus.COMPLETED) {
            return;
        }
        boolean paidContextReady = saga.getStatus() == SagaStatus.PAYMENT_REQUESTED
                && saga.getPaymentId() != null;
        boolean paymentNotRequired = saga.getStatus() == SagaStatus.ORDER_CREATED
                && "PAYMENT_NOT_REQUIRED".equals(saga.getCurrentStep());
        if (!paidContextReady && !paymentNotRequired) {
            throw invalidState(saga);
        }
        checkpoint(saga, SagaStatus.FINALIZING_RESERVATIONS, "COMMITTING_INVENTORY");
    }

    @Transactional
    public void markInventoryCommitted(UUID sagaId, UUID orderId) {
        OrderSaga saga = lock(sagaId);
        requireOrder(saga, orderId);
        if (saga.getStatus() == SagaStatus.INVENTORY_COMMITTED
                || saga.getStatus() == SagaStatus.COMPLETED) {
            return;
        }
        requireStatus(saga, SagaStatus.FINALIZING_RESERVATIONS);
        checkpoint(saga, SagaStatus.INVENTORY_COMMITTED, "INVENTORY_COMMITTED");
    }

    @Transactional
    public void markCompleted(UUID sagaId, UUID orderId) {
        OrderSaga saga = lock(sagaId);
        requireOrder(saga, orderId);
        if (saga.getStatus() == SagaStatus.COMPLETED) {
            return;
        }
        requireStatus(saga, SagaStatus.INVENTORY_COMMITTED);
        Instant now = Instant.now(clock);
        saga.setStatus(SagaStatus.COMPLETED);
        saga.setCurrentStep("RESERVATIONS_FINALIZED");
        saga.setLastErrorCode(null);
        saga.setLastErrorMessage(null);
        saga.setUpdatedAt(now);
        saga.setCompletedAt(now);
    }

    @Transactional
    public void recordFailure(UUID sagaId, UUID orderId, Throwable failure) {
        OrderSaga saga = lock(sagaId);
        requireOrder(saga, orderId);
        if (saga.getStatus() != SagaStatus.FINALIZING_RESERVATIONS
                && saga.getStatus() != SagaStatus.INVENTORY_COMMITTED) {
            throw invalidState(saga);
        }
        saga.setLastErrorCode(truncate(errorCode(failure), 100));
        saga.setLastErrorMessage(truncate(failure.getMessage(), 1000));
        saga.setAttemptCount(Math.addExact(saga.getAttemptCount(), 1));
        saga.setCurrentStep(saga.getStatus() == SagaStatus.FINALIZING_RESERVATIONS
                ? "INVENTORY_COMMIT_RETRY_REQUIRED"
                : "VOUCHER_CONSUME_RETRY_REQUIRED");
        saga.setUpdatedAt(Instant.now(clock));
    }

    private OrderSaga lock(UUID sagaId) {
        return sagas.findByIdForUpdate(sagaId)
                .orElseThrow(() -> new OrderException(
                        HttpStatus.NOT_FOUND, "ORDER_SAGA_NOT_FOUND", "Không tìm thấy Create Order Saga."));
    }

    private void checkpoint(OrderSaga saga, SagaStatus status, String step) {
        saga.setStatus(status);
        saga.setCurrentStep(step);
        saga.setLastErrorCode(null);
        saga.setLastErrorMessage(null);
        saga.setUpdatedAt(Instant.now(clock));
    }

    private void requireOrder(OrderSaga saga, UUID orderId) {
        if (!Objects.equals(saga.getOrderId(), orderId)) {
            throw new OrderException(
                    HttpStatus.CONFLICT,
                    "ORDER_SAGA_ORDER_MISMATCH",
                    "Order không khớp checkpoint Saga.");
        }
    }

    private void requireStatus(OrderSaga saga, SagaStatus expected) {
        if (saga.getStatus() != expected) {
            throw invalidState(saga);
        }
    }

    private OrderException invalidState(OrderSaga saga) {
        return new OrderException(
                HttpStatus.CONFLICT,
                "RESERVATION_FINALIZATION_NOT_READY",
                "Saga chưa sẵn sàng hoàn tất reservation từ trạng thái " + saga.getStatus() + ".");
    }

    private String errorCode(Throwable failure) {
        return failure instanceof OrderException orderFailure
                ? orderFailure.getCode()
                : failure.getClass().getSimpleName();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
