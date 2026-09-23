package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.OrderOperationLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderOperationLogRepository extends JpaRepository<OrderOperationLog, UUID> {
}
