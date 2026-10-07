package com.dynamicmart.engagement_service.repository;

import com.dynamicmart.engagement_service.entity.Review;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByOrderItemId(UUID orderItemId);

    Page<Review> findAllByProductIdAndStatusOrderByCreatedAtDesc(UUID productId, String status, Pageable pageable);

    java.util.List<Review> findAllByProductIdAndStatus(UUID productId, String status);

    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
