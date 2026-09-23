package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.OrderShippingSnapshot;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderShippingSnapshotRepository extends JpaRepository<OrderShippingSnapshot, UUID> {
    Optional<OrderShippingSnapshot> findByOrderId(UUID orderId);
}
