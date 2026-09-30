package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.ProductVariant;
import com.dynamicmart.catalog_service.entity.VariantStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);
    Optional<ProductVariant> findBySkuIgnoreCase(String sku);
    List<ProductVariant> findAllByProductIdOrderBySortOrderAsc(UUID productId);
    List<ProductVariant> findAllByProductIdInOrderByProductIdAscSortOrderAsc(Collection<UUID> productIds);
    List<ProductVariant> findAllByProductIdAndStatusOrderBySortOrderAsc(UUID productId, VariantStatus status);
    List<ProductVariant> findAllByIdIn(Collection<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select variant from ProductVariant variant where variant.id in :ids order by variant.id")
    List<ProductVariant> findAllByIdInForShare(@Param("ids") Collection<UUID> ids);
}
