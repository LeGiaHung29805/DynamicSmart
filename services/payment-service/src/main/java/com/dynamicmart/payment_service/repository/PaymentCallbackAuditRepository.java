package com.dynamicmart.payment_service.repository;

import com.dynamicmart.payment_service.entity.PaymentCallbackAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCallbackAuditRepository extends JpaRepository<PaymentCallbackAudit, UUID> {
    List<PaymentCallbackAudit> findTop100ByPaymentIdOrderByReceivedAtDesc(UUID paymentId);
}
