package com.dynamicmart.engagement_service.messaging.producer;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class ReviewEventPublisher {
    private final StreamBridge streamBridge;

    public ReviewEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void reviewCreated(UUID reviewId, UUID productId, double averageRating, long reviewCount) {
        publish("ReviewCreated", reviewId, productId, averageRating, reviewCount);
    }

    public void reviewHidden(UUID reviewId, UUID productId, double averageRating, long reviewCount) {
        publish("ReviewHidden", reviewId, productId, averageRating, reviewCount);
    }

    private void publish(String eventType, UUID reviewId, UUID productId, double averageRating, long reviewCount) {
        Instant now = Instant.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reviewId", reviewId);
        payload.put("productId", productId);
        payload.put("averageRating", averageRating);
        payload.put("reviewCount", reviewCount);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", UUID.randomUUID());
        envelope.put("eventType", eventType);
        envelope.put("eventVersion", 1);
        envelope.put("producer", "engagement-service");
        envelope.put("aggregateId", reviewId);
        envelope.put("occurredAt", now);
        envelope.put("correlationId", reviewId);
        envelope.put("payload", payload);

        streamBridge.send("reviewEvents-out-0",
                MessageBuilder.withPayload(envelope).setHeader("eventType", eventType).build());
    }
}
