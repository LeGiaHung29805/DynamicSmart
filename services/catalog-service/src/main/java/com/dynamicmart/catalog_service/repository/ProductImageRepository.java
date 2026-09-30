package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.ProductImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    List<ProductImage> findAllByProductIdOrderBySortOrderAsc(UUID productId);
    List<ProductImage> findAllByProductIdInOrderByProductIdAscSortOrderAsc(List<UUID> productIds);
    List<ProductImage> findAllByVariantIdOrderBySortOrderAsc(UUID variantId);
    Optional<ProductImage> findByProductIdAndVariantIdIsNullAndPrimaryTrue(UUID productId);
    Optional<ProductImage> findByVariantIdAndPrimaryTrue(UUID variantId);
}
