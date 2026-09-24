package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.InventoryAdjustment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, UUID> {
    Optional<InventoryAdjustment> findByOperationKey(UUID operationKey);
    Page<InventoryAdjustment> findAllByVariantIdOrderByCreatedAtDesc(UUID variantId, Pageable pageable);
}
