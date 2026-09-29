package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.OrderAddress;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAddressRepository extends JpaRepository<OrderAddress, UUID> {
    Optional<OrderAddress> findByOrderId(UUID orderId);
}
