package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.SagaStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSagaRepository extends JpaRepository<OrderSaga, UUID> {
    Optional<OrderSaga> findByCheckoutSessionId(UUID checkoutSessionId);
    boolean existsByCheckoutSessionId(UUID checkoutSessionId);
    Optional<OrderSaga> findByIdempotencyKey(UUID idempotencyKey);
    Optional<OrderSaga> findByOrderId(UUID orderId);
    Optional<OrderSaga> findByCorrelationId(UUID correlationId);
    List<OrderSaga> findAllByStatusIn(Iterable<SagaStatus> statuses);
}
