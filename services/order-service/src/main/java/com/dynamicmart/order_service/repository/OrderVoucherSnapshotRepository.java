package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.OrderVoucherSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderVoucherSnapshotRepository extends JpaRepository<OrderVoucherSnapshot, UUID> {
    List<OrderVoucherSnapshot> findAllByOrderId(UUID orderId);
}
