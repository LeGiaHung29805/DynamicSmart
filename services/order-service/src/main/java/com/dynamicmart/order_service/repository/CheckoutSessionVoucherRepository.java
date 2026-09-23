package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.CheckoutSessionVoucher;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutSessionVoucherRepository extends JpaRepository<CheckoutSessionVoucher, UUID> {
    List<CheckoutSessionVoucher> findAllByCheckoutSessionId(UUID checkoutSessionId);
    void deleteAllByCheckoutSessionId(UUID checkoutSessionId);
}
