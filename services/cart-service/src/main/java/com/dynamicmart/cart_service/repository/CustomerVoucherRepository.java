package com.dynamicmart.cart_service.repository;

import com.dynamicmart.cart_service.entity.CustomerVoucher;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, UUID> {
    List<CustomerVoucher> findAllByCustomerIdOrderByAssignedAtDesc(UUID customerId);
    Optional<CustomerVoucher> findByCustomerIdAndVoucherId(UUID customerId, UUID voucherId);
    Optional<CustomerVoucher> findByIdAndCustomerIdAndVoucherId(UUID id, UUID customerId, UUID voucherId);
}
