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

/** Persists one Saga checkpoint per short local transaction; callers may perform remote I/O between calls. */
@Service
public class OrderSagaCheckpointService {
    private final OrderSagaRepository sagas;
    private final Clock clock;

    public OrderSagaCheckpointService(OrderSagaRepository sagas, Clock clock) {
        this.sagas = sagas;
        this.clock = clock;
    }

    @Transactional
    public void markVoucherReserved(UUID sagaId, UUID reservationId) {
        OrderSaga saga = lock(sagaId);
        if (saga.getStatus() == SagaStatus.VOUCHER_RESERVED
                || saga.getStatus() == SagaStatus.INVENTORY_RESERVED) {
            requireSameReservation(saga.getVoucherReservationId(), reservationId, "VOUCHER_RESERVATION_MISMATCH");
            return;
        }
        requireStatus(saga, SagaStatus.STARTED);
        saga.setVoucherReservationId(reservationId);
        checkpoint(saga, SagaStatus.VOUCHER_RESERVED, "VOUCHER_RESERVED");
    }

    @Transactional
    public void markInventoryReserved(UUID sagaId, UUID reservationId) {
        if (reservationId == null) {
            throw invalidCheckpoint("INVENTORY_RESERVATION_REQUIRED", "Inventory reservation ID không được để trống.");
        }
        OrderSaga saga = lock(sagaId);
        if (saga.getStatus() == SagaStatus.INVENTORY_RESERVED) {
            requireSameReservation(saga.getInventoryReservationId(), reservationId, "INVENTORY_RESERVATION_MISMATCH");
            return;
        }
        requireStatus(saga, SagaStatus.VOUCHER_RESERVED);
        saga.setInventoryReservationId(reservationId);
        checkpoint(saga, SagaStatus.INVENTORY_RESERVED, "INVENTORY_RESERVED");
    }

    @Transactional
    public void beginCompensation(UUID sagaId, String errorCode, String errorMessage) {
        OrderSaga saga = lock(sagaId);
        if (saga.getStatus() == SagaStatus.COMPENSATING) {
            return;
        }
        if (saga.getStatus() != SagaStatus.STARTED
                && saga.getStatus() != SagaStatus.VOUCHER_RESERVED
                && saga.getStatus() != SagaStatus.INVENTORY_RESERVED) {
            throw invalidState(saga);
        }
        saga.setLastErrorCode(truncate(errorCode, 100));
        saga.setLastErrorMessage(truncate(errorMessage, 1000));
        saga.setAttemptCount(Math.addExact(saga.getAttemptCount(), 1));
        checkpoint(saga, SagaStatus.COMPENSATING, "RELEASING_RESERVATIONS");
    }

    @Transactional
    public void markCompensated(UUID sagaId) {
        OrderSaga saga = lock(sagaId);
        if (saga.getStatus() == SagaStatus.COMPENSATED) {
            return;
        }
        requireStatus(saga, SagaStatus.COMPENSATING);
        Instant now = Instant.now(clock);
        saga.setStatus(SagaStatus.COMPENSATED);
        saga.setCurrentStep("RESERVATIONS_RELEASED");
        saga.setUpdatedAt(now);
        saga.setCompletedAt(now);
    }

    @Transactional
    public void recordCompensationFailure(UUID sagaId, Throwable failure) {
        OrderSaga saga = lock(sagaId);
        requireStatus(saga, SagaStatus.COMPENSATING);
        saga.setLastErrorCode(truncate(errorCode(failure), 100));
        saga.setLastErrorMessage(truncate(failure.getMessage(), 1000));
        saga.setAttemptCount(Math.addExact(saga.getAttemptCount(), 1));
        saga.setCurrentStep("COMPENSATION_RETRY_REQUIRED");
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

    private void requireStatus(OrderSaga saga, SagaStatus expected) {
        if (saga.getStatus() != expected) {
            throw invalidState(saga);
        }
    }

    private void requireSameReservation(UUID existing, UUID supplied, String code) {
        if (!Objects.equals(existing, supplied)) {
            throw invalidCheckpoint(code, "Retry trả về reservation ID khác checkpoint đã lưu.");
        }
    }

    private OrderException invalidState(OrderSaga saga) {
        return invalidCheckpoint(
                "INVALID_SAGA_CHECKPOINT",
                "Không thể ghi checkpoint từ trạng thái Saga " + saga.getStatus() + ".");
    }

    private OrderException invalidCheckpoint(String code, String message) {
        return new OrderException(HttpStatus.CONFLICT, code, message);
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
