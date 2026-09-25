package com.dynamicmart.cart_service.repository;

import com.dynamicmart.cart_service.entity.Cart;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByCustomerIdAndStatus(UUID customerId, String status);
}
