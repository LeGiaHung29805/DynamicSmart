package com.dynamicmart.cart_service.repository;

import com.dynamicmart.cart_service.entity.WishlistItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {
    List<WishlistItem> findAllByWishlistIdOrderByCreatedAtDesc(UUID wishlistId);
    boolean existsByWishlistIdAndProductId(UUID wishlistId, UUID productId);
    void deleteByWishlistIdAndProductId(UUID wishlistId, UUID productId);
}
