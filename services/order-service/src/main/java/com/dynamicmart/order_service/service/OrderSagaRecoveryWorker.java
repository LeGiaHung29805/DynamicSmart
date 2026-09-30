package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.config.SagaRecoveryProperties;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.service.OrderSagaRecoveryLeaseService.RecoveryClaim;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically resumes stale Create Order sagas through their existing idempotent orchestrators. */
@Component
@ConditionalOnProperty(prefix = "app.saga-recovery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderSagaRecoveryWorker {
    private static final Logger log = LoggerFactory.getLogger(OrderSagaRecoveryWorker.class);

    private final OrderSagaRecoveryLeaseService leases;
    private final OrderCreationOrchestrator orderCreation;
    private final OrderCompensationRecoveryService compensation;
    private final SagaRecoveryProperties properties;
    private final String workerId = UUID.randomUUID().toString();
    private final AtomicBoolean running = new AtomicBoolean();

    public OrderSagaRecoveryWorker(
            OrderSagaRecoveryLeaseService leases,
            OrderCreationOrchestrator orderCreation,
            OrderCompensationRecoveryService compensation,
            SagaRecoveryProperties properties) {
        this.leases = leases;
        this.orderCreation = orderCreation;
        this.compensation = compensation;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString = "${app.saga-recovery.fixed-delay:30s}",
            initialDelayString = "${app.saga-recovery.initial-delay:30s}")
    public void scheduledRecovery() {
        recoverBatch();
    }

    public RecoveryRunResult recoverBatch() {
        if (!running.compareAndSet(false, true)) {
            return new RecoveryRunResult(0, 0, true);
        }
        int recovered = 0;
        int failed = 0;
        try {
            for (int index = 0; index < properties.batchSize(); index++) {
                Optional<RecoveryClaim> next = leases.claimNext(workerId);
                if (next.isEmpty()) {
                    break;
                }
                RecoveryClaim claim = next.get();
                try {
                    recover(claim);
                    leases.release(claim.sagaId(), workerId);
                    recovered++;
                } catch (RuntimeException failure) {
                    failed++;
                    try {
                        leases.recordFailure(claim.sagaId(), workerId, claim.attemptCount(), failure);
                    } catch (RuntimeException checkpointFailure) {
                        failure.addSuppressed(checkpointFailure);
                    }
                    log.warn("Order Saga recovery failed for sagaId={} status={}",
                            claim.sagaId(), claim.status(), failure);
                }
            }
            return new RecoveryRunResult(recovered, failed, false);
        } finally {
            running.set(false);
        }
    }

    private void recover(RecoveryClaim claim) {
        if (claim.status() == SagaStatus.COMPENSATING) {
            compensation.resume(claim.sagaId());
            return;
        }
        orderCreation.create(claim.customerId(), claim.checkoutSessionId(), claim.idempotencyKey());
    }

    public record RecoveryRunResult(int recovered, int failed, boolean alreadyRunning) {
    }
}
