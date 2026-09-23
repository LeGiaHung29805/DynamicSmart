package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.OrderStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {
    List<OrderStatusHistory> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);
    boolean existsBySourceEventId(UUID sourceEventId);
}
