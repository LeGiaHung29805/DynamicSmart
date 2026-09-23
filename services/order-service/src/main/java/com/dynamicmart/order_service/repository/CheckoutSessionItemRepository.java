package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.CheckoutSessionItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutSessionItemRepository extends JpaRepository<CheckoutSessionItem, UUID> {
    List<CheckoutSessionItem> findAllByCheckoutSessionId(UUID checkoutSessionId);
    void deleteAllByCheckoutSessionId(UUID checkoutSessionId);
}
