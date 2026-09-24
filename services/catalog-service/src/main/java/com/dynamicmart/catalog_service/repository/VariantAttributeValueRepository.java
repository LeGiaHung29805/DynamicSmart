package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.VariantAttributeValue;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariantAttributeValueRepository extends JpaRepository<VariantAttributeValue, UUID> {
    List<VariantAttributeValue> findAllByVariantId(UUID variantId);
    List<VariantAttributeValue> findAllByVariantIdIn(List<UUID> variantIds);
    Optional<VariantAttributeValue> findByVariantIdAndAttributeId(UUID variantId, UUID attributeId);
    void deleteAllByVariantId(UUID variantId);
}
