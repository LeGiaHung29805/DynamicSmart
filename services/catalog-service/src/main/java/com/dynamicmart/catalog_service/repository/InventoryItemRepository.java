package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventory from InventoryItem inventory where inventory.variantId = :variantId")
    Optional<InventoryItem> findByVariantIdForUpdate(@Param("variantId") UUID variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventory from InventoryItem inventory where inventory.variantId in :variantIds order by inventory.variantId")
    List<InventoryItem> findAllByVariantIdInForUpdate(@Param("variantIds") Collection<UUID> variantIds);

    List<InventoryItem> findAllByVariantIdIn(Collection<UUID> variantIds);
}
