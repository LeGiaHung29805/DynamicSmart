package com.dynamicmart.payment_service.application;

import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentDueConsumer {
    private final PaymentDueService paymentDueService;

    public PaymentDueConsumer(PaymentDueService paymentDueService) { this.paymentDueService = paymentDueService; }

    @Bean
    public Consumer<PaymentDueEvent> paymentDue() { return paymentDueService::consume; }
}
