package com.dynamicmart.engagement_service.repository;

import com.dynamicmart.engagement_service.entity.WishlistItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {
    List<WishlistItem> findAllByWishlistIdOrderByCreatedAtDesc(UUID wishlistId);

    Optional<WishlistItem> findByWishlistIdAndProductId(UUID wishlistId, UUID productId);

    boolean existsByWishlistIdAndProductId(UUID wishlistId, UUID productId);
}
