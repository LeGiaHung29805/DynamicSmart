package com.dynamicmart.payment_service.messaging.producer;

import java.util.Map;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class StreamPaymentEventSender implements PaymentEventSender {
    private final StreamBridge streamBridge;

    public StreamPaymentEventSender(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public boolean send(String eventType, Map<String, Object> envelope) {
        return streamBridge.send("paymentEvents-out-0",
                MessageBuilder.withPayload(envelope).setHeader("eventType", eventType).build());
    }
}
