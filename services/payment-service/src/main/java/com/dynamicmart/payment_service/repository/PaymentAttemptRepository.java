package com.dynamicmart.payment_service.repository;

import com.dynamicmart.payment_service.entity.PaymentAttempt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    List<PaymentAttempt> findByPaymentIdOrderByAttemptNoDesc(UUID paymentId);
    java.util.Optional<PaymentAttempt> findByProviderReference(String providerReference);
}
