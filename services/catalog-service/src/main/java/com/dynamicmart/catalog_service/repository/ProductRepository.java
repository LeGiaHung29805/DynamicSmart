package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.Product;
import com.dynamicmart.catalog_service.entity.ProductStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    boolean existsBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);
    Optional<Product> findBySlugIgnoreCase(String slug);
    Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);
    Page<Product> findAllByCategoryId(UUID categoryId, Pageable pageable);
}
