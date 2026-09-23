package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.AttributeDefinition;
import com.dynamicmart.catalog_service.entity.CatalogStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
    Optional<AttributeDefinition> findByCodeIgnoreCase(String code);
    List<AttributeDefinition> findAllByStatusOrderByNameAsc(CatalogStatus status);
}
