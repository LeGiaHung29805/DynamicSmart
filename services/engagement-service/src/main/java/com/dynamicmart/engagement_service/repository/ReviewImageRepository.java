package com.dynamicmart.engagement_service.repository;

import com.dynamicmart.engagement_service.entity.ReviewImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, UUID> {
    List<ReviewImage> findAllByReviewIdOrderBySortOrderAsc(UUID reviewId);

    List<ReviewImage> findAllByReviewIdInOrderBySortOrderAsc(List<UUID> reviewIds);
}
