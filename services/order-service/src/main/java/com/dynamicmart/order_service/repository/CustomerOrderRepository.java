package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.CustomerOrder;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID>, JpaSpecificationExecutor<CustomerOrder> {
    Optional<CustomerOrder> findByIdAndCustomerId(UUID id, UUID customerId);
    Optional<CustomerOrder> findByCheckoutSessionId(UUID checkoutSessionId);
    Page<CustomerOrder> findAllByCustomerId(UUID customerId, Pageable pageable);
    java.util.List<CustomerOrder> findAllByCustomerIdAndStatus(UUID customerId, com.dynamicmart.order_service.entity.OrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select customerOrder from CustomerOrder customerOrder where customerOrder.id = :id")
    Optional<CustomerOrder> findForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select customerOrder from CustomerOrder customerOrder
            where customerOrder.id = :id and customerOrder.customerId = :customerId
            """)
    Optional<CustomerOrder> findOwnedForUpdate(
            @Param("id") UUID id,
            @Param("customerId") UUID customerId);
}
