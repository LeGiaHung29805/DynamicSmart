package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.InventoryOperationLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryOperationLogRepository extends JpaRepository<InventoryOperationLog, UUID> {
}
