package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.SagaStatus;
import jakarta.persistence.LockModeType;
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
}
