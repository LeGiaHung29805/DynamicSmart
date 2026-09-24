package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.InventoryReservationItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationItemRepository extends JpaRepository<InventoryReservationItem, UUID> {
    List<InventoryReservationItem> findAllByReservationIdOrderByVariantIdAsc(UUID reservationId);
}
