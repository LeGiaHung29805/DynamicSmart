package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.AttributeOption;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeOptionRepository extends JpaRepository<AttributeOption, UUID> {
    boolean existsByAttributeIdAndCodeIgnoreCase(UUID attributeId, String code);
    boolean existsByAttributeIdAndCodeIgnoreCaseAndIdNot(UUID attributeId, String code, UUID id);
    List<AttributeOption> findAllByAttributeIdOrderBySortOrderAscLabelAsc(UUID attributeId);
    List<AttributeOption> findAllByAttributeIdInOrderByAttributeIdAscSortOrderAscLabelAsc(
            List<UUID> attributeIds);
}
