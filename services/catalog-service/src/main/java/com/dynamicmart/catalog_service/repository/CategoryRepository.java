package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.CatalogStatus;
import com.dynamicmart.catalog_service.entity.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
    boolean existsBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);
    Optional<Category> findBySlugIgnoreCase(String slug);
    List<Category> findAllByOrderBySortOrderAscNameAsc();
    List<Category> findAllByStatusOrderBySortOrderAscNameAsc(CatalogStatus status);
    List<Category> findAllByParentIdOrderBySortOrderAscNameAsc(UUID parentId);
}
