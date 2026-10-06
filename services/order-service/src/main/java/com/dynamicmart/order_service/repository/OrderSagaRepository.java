package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.SagaStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderSagaRepository extends JpaRepository<OrderSaga, UUID> {
    Optional<OrderSaga> findByCheckoutSessionId(UUID checkoutSessionId);
    boolean existsByCheckoutSessionId(UUID checkoutSessionId);
    Optional<OrderSaga> findByIdempotencyKey(UUID idempotencyKey);
    Optional<OrderSaga> findByOrderId(UUID orderId);
    Optional<OrderSaga> findByCorrelationId(UUID correlationId);
    List<OrderSaga> findAllByStatusIn(Iterable<SagaStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select saga from OrderSaga saga where saga.id = :id")
    Optional<OrderSaga> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Claims are serialized with PostgreSQL SKIP LOCKED so multiple Order Service instances can
     * recover different sagas without waiting for, or duplicating, another worker's selection.
     * PAYMENT_REQUESTED is eligible only for a confirmed Order; prepaid Orders legitimately wait
     * in PENDING_PAYMENT until the Payment event arrives.
     */
    @Query(value = """
            select saga.*
            from order_sagas saga
            where saga.updated_at <= :staleBefore
              and (saga.recovery_lease_until is null or saga.recovery_lease_until <= :now)
              and (
                    saga.status in (
                        'STARTED', 'VOUCHER_RESERVED', 'INVENTORY_RESERVED', 'ORDER_CREATED',
                        'FINALIZING_RESERVATIONS', 'INVENTORY_COMMITTED', 'COMPENSATING'
                    )
                    or (
                        saga.status = 'PAYMENT_REQUESTED'
                        and exists (
                            select 1 from orders customer_order
                            where customer_order.id = saga.order_id
                              and customer_order.status = 'CONFIRMED'
                        )
                    )
              )
            order by saga.updated_at, saga.id
            limit 1
            for update skip locked
            """, nativeQuery = true)
    Optional<OrderSaga> findNextRecoveryCandidateForUpdate(
            @Param("staleBefore") Instant staleBefore,
            @Param("now") Instant now);
}
