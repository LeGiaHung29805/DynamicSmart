package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.ProductAttributeValue;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, UUID> {
    List<ProductAttributeValue> findAllByProductId(UUID productId);
    List<ProductAttributeValue> findAllByProductIdIn(List<UUID> productIds);
    Optional<ProductAttributeValue> findByProductIdAndAttributeId(UUID productId, UUID attributeId);
    void deleteAllByProductId(UUID productId);
}
