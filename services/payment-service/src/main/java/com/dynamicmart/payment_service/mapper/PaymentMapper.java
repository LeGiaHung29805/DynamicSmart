package com.dynamicmart.payment_service.mapper;

import com.dynamicmart.payment_service.dto.response.PaymentAttemptResponse;
import com.dynamicmart.payment_service.dto.response.PaymentCallbackAuditResponse;
import com.dynamicmart.payment_service.dto.response.PaymentResponse;
import com.dynamicmart.payment_service.entity.Payment;
import com.dynamicmart.payment_service.entity.PaymentAttempt;
import com.dynamicmart.payment_service.entity.PaymentCallbackAudit;

public final class PaymentMapper {
    private PaymentMapper() { }

    public static PaymentResponse toResponse(Payment payment, String redirectUrl) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmountVnd(),
                payment.getTiming(), payment.getMethod(), payment.getStatus(), redirectUrl,
                payment.getExpiresAt(), payment.getPaidAt(), payment.getCodReceiptNo(),
                payment.getCodConfirmedBy(), payment.getCodConfirmedAt());
    }

    public static PaymentAttemptResponse toAttemptResponse(PaymentAttempt attempt) {
        return new PaymentAttemptResponse(attempt.getId(), attempt.getAttemptNo(), attempt.getProvider(),
                attempt.getProviderReference(), attempt.getAmountVnd(), attempt.getStatus(),
                attempt.getExpiresAt(), attempt.getCreatedAt());
    }

    public static PaymentCallbackAuditResponse toAuditResponse(PaymentCallbackAudit audit) {
        return new PaymentCallbackAuditResponse(audit.getId(), audit.getProviderTransactionRef(),
                audit.isChecksumValid(), audit.isAmountValid(), audit.getProcessedResult(), audit.getReceivedAt());
    }
}
