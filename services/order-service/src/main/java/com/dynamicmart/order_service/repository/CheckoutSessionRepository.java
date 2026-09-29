package com.dynamicmart.order_service.repository;
import com.dynamicmart.order_service.entity.CheckoutSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, UUID> { }
