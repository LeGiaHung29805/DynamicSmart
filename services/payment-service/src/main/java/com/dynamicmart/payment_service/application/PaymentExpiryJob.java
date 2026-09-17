package com.dynamicmart.payment_service.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentExpiryJob {
    private final PaymentService paymentService;
    public PaymentExpiryJob(PaymentService paymentService) { this.paymentService = paymentService; }

    @Scheduled(fixedDelayString = "${app.vnpay.expiry-scan-delay-ms:60000}")
    public void expirePrepaidPayments() { paymentService.expireDuePrepaidPayments(); }
}
