package com.dynamicmart.order_service.repository;
import com.dynamicmart.order_service.entity.CustomerOrder;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> { }
