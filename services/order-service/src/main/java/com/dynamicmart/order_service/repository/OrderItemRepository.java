package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.OrderItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findAllByOrderId(UUID orderId);
}
