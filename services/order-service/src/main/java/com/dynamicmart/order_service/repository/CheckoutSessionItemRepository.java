package com.dynamicmart.order_service.repository;
import com.dynamicmart.order_service.entity.CheckoutSessionItem;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CheckoutSessionItemRepository extends JpaRepository<CheckoutSessionItem, UUID> {
    List<CheckoutSessionItem> findAllByCheckoutSessionId(UUID checkoutSessionId);
}
