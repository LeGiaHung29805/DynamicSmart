package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.config.SagaRecoveryProperties;
import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns the short database transactions used to claim and release recovery work. */
@Service
public class OrderSagaRecoveryLeaseService {
    private final OrderSagaRepository sagas;
    private final CheckoutSessionRepository sessions;
    private final SagaRecoveryProperties properties;
    private final Clock clock;

    public OrderSagaRecoveryLeaseService(
            OrderSagaRepository sagas,
            CheckoutSessionRepository sessions,
            SagaRecoveryProperties properties,
            Clock clock) {
        this.sagas = sagas;
        this.sessions = sessions;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public Optional<RecoveryClaim> claimNext(String workerId) {
        requireWorkerId(workerId);
        Instant now = Instant.now(clock);
        OrderSaga saga = sagas.findNextRecoveryCandidateForUpdate(
                        now.minus(properties.staleAfter()), now)
                .orElse(null);
        if (saga == null) {
            return Optional.empty();
        }
        CheckoutSession session = sessions.findById(saga.getCheckoutSessionId())
                .orElseThrow(() -> new OrderException(
                        HttpStatus.CONFLICT,
                        "RECOVERY_CHECKOUT_SESSION_MISSING",
                        "Saga cần recovery nhưng Checkout Session không còn tồn tại."));
        saga.setRecoveryOwner(workerId);
        saga.setRecoveryLeaseUntil(now.plus(properties.leaseDuration()));
        return Optional.of(new RecoveryClaim(
                saga.getId(), saga.getStatus(), session.getCustomerId(), saga.getCheckoutSessionId(),
                saga.getIdempotencyKey(), saga.getOrderId(), saga.getAttemptCount()));
    }

    @Transactional
    public void release(UUID sagaId, String workerId) {
        OrderSaga saga = lock(sagaId);
        if (!Objects.equals(workerId, saga.getRecoveryOwner())) {
            return;
        }
        saga.setRecoveryOwner(null);
        saga.setRecoveryLeaseUntil(null);
    }

    @Transactional
    public void recordFailure(UUID sagaId, String workerId, int attemptCountAtClaim, Throwable failure) {
        OrderSaga saga = lock(sagaId);
        if (!Objects.equals(workerId, saga.getRecoveryOwner())) {
            return;
        }
        Instant now = Instant.now(clock);
        saga.setRecoveryOwner(null);
        saga.setRecoveryLeaseUntil(now.plus(properties.retryBackoff()));
        saga.setLastErrorCode(truncate(errorCode(failure), 100));
        saga.setLastErrorMessage(truncate(failure == null ? null : failure.getMessage(), 1000));
        if (saga.getAttemptCount() == attemptCountAtClaim) {
            saga.setAttemptCount(Math.addExact(saga.getAttemptCount(), 1));
        }
        saga.setUpdatedAt(now);
    }

    private OrderSaga lock(UUID sagaId) {
        return sagas.findByIdForUpdate(sagaId)
                .orElseThrow(() -> new OrderException(
                        HttpStatus.NOT_FOUND, "ORDER_SAGA_NOT_FOUND", "Không tìm thấy Create Order Saga."));
    }

    private void requireWorkerId(String workerId) {
        if (workerId == null || workerId.isBlank() || workerId.length() > 100) {
            throw new IllegalArgumentException("Recovery worker ID phải có độ dài 1..100 ký tự.");
        }
    }

    private String errorCode(Throwable failure) {
        if (failure instanceof OrderException orderFailure) {
            return orderFailure.getCode();
        }
        return failure == null ? "UNKNOWN_RECOVERY_FAILURE" : failure.getClass().getSimpleName();
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record RecoveryClaim(
            UUID sagaId,
            SagaStatus status,
            UUID customerId,
            UUID checkoutSessionId,
            UUID idempotencyKey,
            UUID orderId,
            int attemptCount) {
    }
}
