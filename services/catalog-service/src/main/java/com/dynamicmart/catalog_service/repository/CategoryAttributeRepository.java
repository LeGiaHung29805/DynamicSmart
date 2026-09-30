package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.AttributeAppliesTo;
import com.dynamicmart.catalog_service.entity.CategoryAttribute;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, UUID> {
    boolean existsByCategoryIdAndAttributeIdAndAppliesTo(UUID categoryId, UUID attributeId,
                                                         AttributeAppliesTo appliesTo);
    Optional<CategoryAttribute> findByCategoryIdAndAttributeIdAndAppliesTo(
            UUID categoryId, UUID attributeId, AttributeAppliesTo appliesTo);
    List<CategoryAttribute> findAllByCategoryIdOrderBySortOrderAsc(UUID categoryId);
    List<CategoryAttribute> findAllByCategoryIdInOrderBySortOrderAsc(List<UUID> categoryIds);
}
