package com.dynamicmart.payment_service.messaging.consumer;

import com.dynamicmart.payment_service.service.PaymentDueService;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentDueConsumer {
    private final PaymentDueService paymentDueService;

    public PaymentDueConsumer(PaymentDueService paymentDueService) { this.paymentDueService = paymentDueService; }

    @Bean
    public Consumer<PaymentDueMessage> paymentDue() {
        return message -> {
            if (message != null && message.isPaymentDue()) {
                paymentDueService.consume(message.toDomainEvent());
            }
        };
    }
}
