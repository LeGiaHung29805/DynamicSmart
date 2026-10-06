package com.dynamicmart.order_service.messaging;

import com.dynamicmart.order_service.service.OrderPaymentEventHandler;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentEventConsumerConfiguration {
    @Bean
    Consumer<PaymentEventEnvelope> paymentEvents(OrderPaymentEventHandler handler) {
        return handler::handle;
    }
}
