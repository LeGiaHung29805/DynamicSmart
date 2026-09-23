package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.ProductVariant;
import com.dynamicmart.catalog_service.entity.VariantStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);
    Optional<ProductVariant> findBySkuIgnoreCase(String sku);
    List<ProductVariant> findAllByProductIdOrderBySortOrderAsc(UUID productId);
    List<ProductVariant> findAllByProductIdAndStatusOrderBySortOrderAsc(UUID productId, VariantStatus status);
    List<ProductVariant> findAllByIdIn(Collection<UUID> ids);
}
