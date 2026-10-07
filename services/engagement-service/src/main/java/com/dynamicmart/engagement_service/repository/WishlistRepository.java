package com.dynamicmart.engagement_service.repository;

import com.dynamicmart.engagement_service.entity.Wishlist;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    Optional<Wishlist> findByCustomerId(UUID customerId);
}
