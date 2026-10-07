package com.dynamicmart.engagement_service.repository;

import com.dynamicmart.engagement_service.entity.ProductQuestion;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, UUID> {
    Page<ProductQuestion> findAllByProductIdAndStatusInOrderByCreatedAtDesc(
            UUID productId, Collection<String> statuses, Pageable pageable);

    Page<ProductQuestion> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Page<ProductQuestion> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
