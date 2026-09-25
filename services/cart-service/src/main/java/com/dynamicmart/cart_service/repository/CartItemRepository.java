package com.dynamicmart.cart_service.repository;

import com.dynamicmart.cart_service.entity.CartItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findAllByCartIdOrderByCreatedAtAsc(UUID cartId);
    Optional<CartItem> findByCartIdAndVariantId(UUID cartId, UUID variantId);
    Optional<CartItem> findByIdAndCartId(UUID id, UUID cartId);
}
