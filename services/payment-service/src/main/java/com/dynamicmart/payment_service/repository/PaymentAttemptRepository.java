package com.dynamicmart.payment_service.repository;

import com.dynamicmart.payment_service.entity.PaymentAttempt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    List<PaymentAttempt> findByPaymentIdOrderByAttemptNoDesc(UUID paymentId);
    java.util.Optional<PaymentAttempt> findByProviderReference(String providerReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PaymentAttempt a where a.providerReference = :providerReference")
    java.util.Optional<PaymentAttempt> findByProviderReferenceForUpdate(@Param("providerReference") String providerReference);
}
